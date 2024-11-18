/*
 * Copyright 2020 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package score;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Event;
import com.iconloop.score.test.ManualRevertException;
import com.iconloop.score.test.OutOfBalanceException;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TExternal;
import com.iconloop.score.test.TOptional;
import com.iconloop.score.test.TScore;
import com.iconloop.score.test.WorldState;
import score.impl.AnyDBImpl;
import score.impl.Crypto;
import score.impl.TypeConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

class ServiceManagerImpl extends ServiceManager implements AnyDBImpl.ValueStore {
    private static final BigInteger ICX = BigInteger.TEN.pow(18);

    private final Stack<Frame> contexts = new Stack<>();
    private int nextCount = 0xff;   /* 00 ~ ff is reserved for system contracts */
    private final WorldState state = new WorldState();
    private final Map<Address,Account> accounts = new HashMap<>();

    private static final ThreadLocal<TransactionInfo> txInfo = new ThreadLocal<>();

    private EventLogger eventLogger = null;
    private List<Event> lastLogs = null;

    static class TransactionInfo {
        private final int index;
        private final byte[] txHash;
        private final Block block;
        private final long ts;

        private static final int kInvalidIndex = -1;

        TransactionInfo(Block blk, int index) {
            this.block = blk;
            if (index >= 0) {
                this.index = index;
                this.txHash = blk.hashOfTransactionAt(index);
                this.ts = blk.getTimestamp()+index*10;
            } else {
                this.index = kInvalidIndex;
                this.txHash = null;
                this.ts = 0;
            }
        }

        public byte[] getHash() {
            return txHash != null ? Arrays.copyOf(txHash, txHash.length) : null;
        }

        public int getIndex() {
            if (index < 0) {
                throw new IllegalStateException("NotInTransaction");
            }
            return index;
        }

        public long getTimestamp() {
            if (index < 0) {
                throw new IllegalStateException("NotInTransaction");
            }
            return ts;
        }

        public TransactionInfo next() {
            return new TransactionInfo(block, index+1);
        }
    }

    private interface TXIScope extends AutoCloseable {
        void close();
    }

    private TXIScope setupTransactionInfo(boolean forTx) {
        var txi = txInfo.get();
        if (txi == null) {
            txInfo.set(new TransactionInfo(
                    Block.next(),
                    forTx ? 0 : TransactionInfo.kInvalidIndex
            ));
            eventLogger = new EventLogger();
            return () -> {
                txInfo.set(null);
                lastLogs = eventLogger.getLogs();
                eventLogger = null;
            };
        } else if (forTx) {
            txInfo.set(txi.next());
            eventLogger = new EventLogger();
        }
        return () -> {
            lastLogs = eventLogger.getLogs();
            eventLogger = null;
        };
    }

    @Override
    public Score deploy(Account caller, Class<?> mainClass, Object... params) throws Exception {
        try (var scope = setupTransactionInfo(true)) {
            return deploy(caller, null, mainClass, params);
        }
    }

    private Score deploy(Account caller, Score score, Class<?> mainClass, Object[] params) throws Exception {
        if (score == null) {
            var acct = createScoreAccount();
            score = new Score(acct, caller);
        } else {
            if (score.getOwner() != caller) {
                throw new RevertedException("NoPermissionToUpdate(owner="+score.getOwner().getAddress()+",caller="+caller.getAddress());
            }
        }
        pushFrame(caller, score.getAccount(), false, "<init>", BigInteger.ZERO);
        state.setScore(score.getAddress(), score);
        try {
            Constructor<?>[] ctor = mainClass.getConstructors();
            if (ctor.length != 1) {
                // User SCORE should only have one public constructor
                throw new AssertionError("multiple public constructors found");
            }
            var readonly = checkAnnotationsAndReadOnly(mainClass, ctor[0], params.length, false, false, false);
            if (readonly) {
                throw new IllegalStateException("ExternalReadOnlyConstructor(class="+mainClass+")");
            }
            var params2 = convertParameters(ctor[0], params);
            score.setInstance(ctor[0].newInstance(params2));
            applyFrame();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw e;
        } finally {
            popFrame();
        }
        return score;
    }

    private Address nextAddress(boolean isContract) {
        var ba = new byte[Address.LENGTH];
        ba[0] = isContract ? (byte)1 :(byte)0;
        var seed = ++nextCount;
        var index = ba.length - 1;
        ba[index--] = (byte) seed;
        ba[index--] = (byte) (seed >> 8);
        ba[index--] = (byte) (seed >> 16);
        ba[index] = (byte) (seed >> 24);
        return new Address(ba);
    }

    Account createAccount(Address address) {
        if (address==null) {
            throw new NullPointerException("AddressIsNull");
        }
        if (accounts.containsKey(address)) {
            throw new IllegalArgumentException("AlreadyCreatedAccount(addr="+address+")");
        }
        var acct = new Account(state, address);
        accounts.put(address, acct);
        return acct;
    }

    @Override
    public Account createAccount() {
        return createAccount(nextAddress(false));
    }

    @Override
    public Account createAccount(int initialIcx) {
        var acct = createAccount();
        acct.addBalance(ICX.multiply(BigInteger.valueOf(initialIcx)));
        return acct;
    }

    @Override
    public Account getAccount(Address addr) {
        if (addr == null) {
            throw new NullPointerException(("AddressIsNull"));
        }
        if (accounts.containsKey(addr)) {
            return accounts.get(addr);
        }
        return createAccount(addr);
    }

    @Override
    public Account createScoreAccount() {
        return new Account(state, nextAddress(true));
    }

    public Score deploy(Address addr, Account owner, Object instance) {
        if (!addr.isContract()) {
            throw new IllegalArgumentException("AddressMustBeContract");
        }

        var account = getAccount(addr);
        var score = new Score(account, owner);
        score.setInstance(instance);
        state.setScore(addr, score);
        return score;
    }

    Address getOwner() {
        var address = getCurrentFrame().to.getAddress();
        return getScoreFromAddress(address).getOwner().getAddress();
    }

    Address getOrigin() {
        return getFirstFrame().from.getAddress();
    }

    Address getCaller() {
        return getCurrentFrame().from.getAddress();
    }

    Address getAddress() {
        return getCurrentFrame().to.getAddress();
    }

    Account getTarget() { return getCurrentFrame().to; }

    private Score getScoreFromAddress(Address target) {
        var score = state.getScore(target);
        if (score == null) {
            throw new IllegalArgumentException("ContractNotFound(addr="+target+")");
        }
        return score;
    }

    @Override
    public void invoke(Account from, BigInteger value, Address targetAddress, String method, Object... params) {
        try (var scope = setupTransactionInfo(true)) {
            handleCall(from, value, true, false, targetAddress, method, params);
        }
    }

    @Override
    public Object call(Account from, BigInteger value, Address targetAddress, String method, Object... params) {
        if (from == null) {
            throw new NullPointerException("from is null");
        }
        try (var scope = setupTransactionInfo(true)) {
            return handleCall(from, value, true, false, targetAddress, method, params);
        }
    }

    @Override
    public Object call(Address targetAddress, String method, Object... params) {
        return handleCall(null, BigInteger.ZERO, false, true, targetAddress, method, params);
    }

    @Override
    public <T> T call(Class<T> cls, Address targetAddress, String method, Object... params) {
        return TypeConverter.cast(
                handleCall(null, BigInteger.ZERO, false, true,
                        targetAddress, method, params
                ),
                cls
        );
    }

    Object call(BigInteger value, Address targetAddress, String method, Object... params) {
        if (targetAddress.isContract()) {
            Score from = getScoreFromAddress(getAddress());
            if ("".equals(method)) {
                handleTransfer(from.getAccount(), targetAddress, value);
                return null;
            } else {
                return handleCall(from.getAccount(), value, true, false, targetAddress, method, params);
            }
        } else {
            handleTransfer(getTarget(), targetAddress, value);
            return null;
        }
    }

    private Object handleCall(Account from, BigInteger value, boolean transfer, boolean readonly, Address targetAddress, String method, Object... params) {
        if (value.signum()<0) {
            throw new IllegalArgumentException("value is negative");
        }
        Score score = getScoreFromAddress(targetAddress);
        Method scoreMethod;
        var scoreClass = score.getInstance().getClass();
        try {
            scoreMethod = getMethodByName(scoreClass, method);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "NoValidMethod(score="+score.getAddress()+",method="+method+")");
        }
        readonly = checkAnnotationsAndReadOnly(scoreClass, scoreMethod, params.length,
                true, isReadonly()|readonly, value.signum()>0);
        Account to = score.getAccount();
        pushFrame(from, to, readonly, method, value);
        try {
            if (value.signum()>0 && transfer) {
                from.subtractBalance(value);
                to.addBalance(value);
            }
            var obj = invokeMethod(score, method, params);
            applyFrame();
            return obj;
        } finally {
            popFrame();
        }
    }

    int getTransactionIndex() {
        var info = txInfo.get();
        return info != null ? info.getIndex() : 0;
    }

    byte[] getTransactionHash() {
        var info = txInfo.get();
        return info != null ? info.getHash() : null;
    }

    long getTransactionTimestamp() {
        var info = txInfo.get();
        return info != null ? info.getTimestamp() : System.currentTimeMillis() * 1000;
    }

    @Override
    public void transfer(Account from, Address targetAddress, BigInteger value) {
        try (var scope = setupTransactionInfo(true)) {
            handleTransfer(from, targetAddress, value);
        }
    }

    private void handleTransfer(Account from, Address targetAddress, BigInteger value) {
        var fromBalance = from.getBalance();
        if (fromBalance.compareTo(value) < 0) {
            throw new OutOfBalanceException("OutOfBalance(from="+from.getAddress()+",balance="+fromBalance+",value="+value+")");
        }
        var to = getAccount(targetAddress);
        try {
            state.push();
            from.subtractBalance(value);
            to.addBalance(value);
            if (targetAddress.isContract()) {
                handleCall(from, value, false, false, targetAddress, "fallback");
            }
            state.apply();
        } finally {
            state.pop();
        }
    }

    @Override
    public <T> T getValue(Class<T> cls, Address address, String key) {
        return TypeConverter.fromBytes(cls, state.getValue(address, key));
    }

    @Override
    public void setValue(Address address, String key, Object value) {
        if (isReadonly()) {
            throw new IllegalStateException("SetValueInReadOnly(addr="+address+",key="+key+",value="+value+")");
        }
        state.setValue(address, key, TypeConverter.toBytes(value));
    }

    /**
     * Get value of the storage of the current contract
     * @param cls Expecting object class
     * @param key Key for storage.
     * @return Deserialized value
     * @param <T> Expecting object class
     */
    @Override
    public <T> T getValue(Class<T> cls, String key) {
        return getValue(cls, getAddress(), key);
    }

    /**
     * Set Value of the storage of the current contract
     * @param key Key for storage
     * @param value New value to store
     */
    @Override
    public void setValue(String key, Object value) {
        setValue(getAddress(), key, value);
    }

    public static class Block implements ServiceManager.Block {
        private static Block sLast;

        // 2 seconds ( 2_000_000 micro-seconds )
        private static long sBlockInterval = 2_000_000;

        private final long height;
        private final long timestamp;

        private Block(long height, long timestamp) {
            this.height = height;
            this.timestamp = timestamp;
        }

        public static Block getLast() {
            if (sLast == null) {
                Random rand = new Random();
                sLast = new Block(rand.nextInt(1000), System.currentTimeMillis() * 1000);
            }
            return sLast;
        }

        public long getHeight() {
            return height;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void increase() {
            next(1);
        }

        public void increase(long delta) {
            next(delta);
        }

        public String toString() {
            return "Block(height="+height+",ts="+timestamp+")";
        }

        public byte[] hashOfTransactionAt(int idx) {
            return Crypto.sha3_256((this+":"+idx).getBytes());
        }

        static Block next() {
            return next(1);
        }

        static Block next(long delta) {
            return next(delta, delta*sBlockInterval);
        }

        static Block next(long delta, long duration) {
            if (txInfo.get() != null) {
                throw new IllegalStateException("NotAllowedToAdvanceBlock");
            }
            if (delta <= 0) {
                throw new IllegalArgumentException("InvalidHeightDelta(delta=" + delta + ")");
            }
            if (duration <= 0) {
                throw new IllegalArgumentException("InvalidBlockDuration(duration=" + delta + ")");
            }
            var last = getLast();
            sLast = new Block(last.height + delta, last.timestamp + duration);
            return sLast;
        }
    }

    public ServiceManager.Block getBlock() {
        return Block.getLast();
    }

    public static class Frame {
        Account from;
        Account to;
        String method;
        boolean readonly;
        BigInteger value;

        public Frame(Account from, Account to, boolean readonly, String method, BigInteger value) {
            this.from = from;
            this.to = to;
            this.readonly = readonly;
            this.method = method;
            this.value = value;
        }

        public boolean isReadonly() {
            return readonly;
        }

        public BigInteger getValue() {
            return value;
        }
    }

    private boolean isReadonly() {
        return contexts.empty() ? false : getCurrentFrame().isReadonly();
    }

    protected void pushFrame(Account from, Account to, boolean readonly, String method, BigInteger value) {
        contexts.push(new Frame(from, to, isReadonly() || readonly, method, value));
        state.push();
        if (eventLogger != null) eventLogger.push();
    }

    protected void popFrame() {
        if (eventLogger != null) eventLogger.pop();
        state.pop();
        contexts.pop();
    }

    void applyFrame() {
        if (eventLogger != null) eventLogger.apply();
        state.apply();
    }

    Frame getCurrentFrame() {
        return contexts.peek();
    }

    Frame getFirstFrame() {
        return contexts.firstElement();
    }

    private static ServiceManagerImpl instance;

    private ServiceManagerImpl() { }

    static ServiceManagerImpl getServiceManagerImpl() {
        if (instance == null) {
            instance = new ServiceManagerImpl();
        }
        return instance;
    }

    public static ServiceManager getServiceManager() {
        return getServiceManagerImpl();
    }

    private static Object defaultValueFor(Class<?> clz) {
        if (clz==boolean.class) {
            return false;
        } else if (clz==byte.class || clz==Byte.class) {
            return (byte) 0x00;
        } else if (clz==char.class || clz==Character.class) {
            return (char) 0x00;
        } else if (clz==short.class || clz==Short.class) {
            return (short) 0x00;
        } else if (clz==int.class || clz==Integer.class) {
            return 0;
        } else if (clz==long.class || clz==Long.class) {
            return 0L;
        } else if (clz==BigInteger.class) {
            return BigInteger.ZERO;
        }
        return null;
    }

    private static Object[] convertParameters(Executable method, Object[] params) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        int numberOfParams = parameterTypes.length;
        Object[] parsedParams = new Object[numberOfParams];

        var scoreClz = method.getDeclaringClass();
        if (scoreClz.isAnnotationPresent(TScore.class)) {
            var annotations = method.getParameterAnnotations();
            int minParams = annotations.length;
            for (int i=0 ; i<annotations.length ; i++) {
                if (Arrays.stream(annotations[i])
                        .anyMatch((a)->(a.annotationType().equals(TOptional.class)))) {
                    if (i < minParams) {
                        minParams = i;
                    } else {
                        throw new IllegalArgumentException(
                                "InvalidOptionalTag(class=" + method.getDeclaringClass().getName() + ",method=" + method + ")"
                        );
                    }
                }
            }
            if (params.length < minParams) {
                throw new IllegalArgumentException(
                        String.format("NotEnoughParameter(given=%d,min=%d)",
                                params.length, minParams
                        )
                );
            }
        }

        int i = 0;
        for (Class<?> parameterClass : parameterTypes) {
            if (parameterClass == Map.class || parameterClass == List.class) {
                throw new IllegalArgumentException(
                        String.format("ProhibitedParameterType(idx=%d,target=%s)",
                                i, parameterClass.getName()));
            }
            if (i>=params.length) {
                parsedParams[i] = defaultValueFor(parameterClass);
            } else {
                try {
                    parsedParams[i] = TypeConverter.cast(params[i], parameterClass);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                            String.format("InvalidParameter(idx=%d,target=%s,source=%s)",
                                    i, parameterClass.getName(), params[i].getClass().getName()), e);
                }
            }
            i++;
        }
        return parsedParams;
    }


    private boolean checkAnnotationsAndReadOnly(Class<?> scoreClz, Executable method, int params, boolean external, boolean readonly, boolean payable) {
        if (!scoreClz.isAnnotationPresent(TScore.class)) {
            return false;
        }
        var externalAnnotation = method.getAnnotation(TExternal.class);
        if (externalAnnotation==null) {
            if (external) {
                throw new IllegalArgumentException(
                        "NotExternal(score="+scoreClz.getName()+",method="+method+")"
                );
            }
        } else {
            if (readonly && !externalAnnotation.readonly()) {
                throw new IllegalArgumentException(
                        "PermissionDenied(score="+
                                scoreClz.getName()+",method="+method+")"
                );
            }
            readonly |= externalAnnotation.readonly();
            if (payable && !externalAnnotation.payable()) {
                throw new IllegalArgumentException(
                        "NotPayable(score="+
                                scoreClz.getName()+",method="+method+")"
                );
            }
        }
        var annotations = method.getParameterAnnotations();
        int minParams = annotations.length;
        for (int i=0 ; i<annotations.length ; i++) {
            if (Arrays.stream(annotations[i])
                    .anyMatch((a)->(a.annotationType().equals(TOptional.class)))) {
                if (i < minParams) {
                    minParams = i;
                } else {
                    throw new IllegalArgumentException(
                            "InvalidOptionalTag(class=" + scoreClz.getName() + ",method=" + method + ")"
                    );
                }
            }
        }
        if (params < minParams) {
            throw new IllegalArgumentException(
                    String.format("NotEnoughParameter(score=%s,method=%s,given=%d,min=%d)",
                            scoreClz.getName(),
                            method.toString(),
                            params, minParams
                    )
            );
        }
        return readonly;
    }

    private Object invokeMethod(Score score, String methodName, Object[] params) {
        try {
            var scoreObj = score.getInstance();
            Method method = getMethodByName(scoreObj.getClass(), methodName);
            Object[] methodParameters = convertParameters(method, params);
            return method.invoke(scoreObj, methodParameters);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "NoValidMethod(score="+score.getAddress()+",method="+methodName+")");
        } catch (InvocationTargetException e) {
            var target = e.getCause();
            if (target instanceof UserRevertException) {
                // custom exception class by contract
                var ure = (UserRevertException) target;
                throw new UserRevertedException(ure.getCode(), target.getMessage(), target);
            } else if (target instanceof ManualRevertException) {
                // exception made by Context.revert() APIs
                var cre = (ManualRevertException) target;
                throw new UserRevertedException(cre.getCode(), target.getMessage(), target);
            }
            throw new RevertedException(target.getMessage(), target);
        }
    }

    private static Method getMethodByName(Class<?> clazz, String name) throws NoSuchMethodException {
        Method[] m = clazz.getMethods();
        for (Method method : m) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new NoSuchMethodException("NoSuchMethod(class="+clazz+",method="+name+")");
    }

    public void logEvent(Object[] indexed, Object[] data) {
        if (isReadonly() || eventLogger == null) {
            throw new IllegalStateException("ReadOnly mode");
        }
        eventLogger.addLog(new Event(getAddress(), indexed, data));
    }

    @Override
    public List<Event> getLastEventLogs() {
        return lastLogs;
    }
}
