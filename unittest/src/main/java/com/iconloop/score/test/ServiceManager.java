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

package com.iconloop.score.test;

import score.Address;
import score.RevertedException;
import score.impl.TypeConverter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.util.Random;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class ServiceManager {
    private static final BigInteger ICX = BigInteger.TEN.pow(18);

    private final Stack<Frame> contexts = new Stack<>();
    private int nextCount = 1;
    private final WorldState state = new WorldState();

    public Score deploy(Account caller, Class<?> mainClass, Object... params) throws Exception {
        getBlock().increase();
        return deploy(caller, null, mainClass, params);
    }

    private Score deploy(Account caller, Score score, Class<?> mainClass, Object[] params) throws Exception {
        if (score == null) {
            var acct = newScoreAccount();
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
            score.setInstance(ctor[0].newInstance(params));
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
        var seed = nextCount++;
        var index = ba.length - 1;
        ba[index--] = (byte) seed;
        ba[index--] = (byte) (seed >> 8);
        ba[index--] = (byte) (seed >> 16);
        ba[index] = (byte) (seed >> 24);
        return new Address(ba);
    }

    public Account createAccount() {
        return new Account(state, nextAddress(false));
    }

    public Account createAccount(int initialIcx) {
        var acct = createAccount();
        acct.addBalance(ICX.multiply(BigInteger.valueOf(initialIcx)));
        return acct;
    }

    public Account getAccount(Address addr) {
        return Account.accountOf(state, addr);
    }

    private Account newScoreAccount() {
        return new Account(state, nextAddress(true));
    }

    public Address getOwner() {
        var address = getCurrentFrame().to.getAddress();
        return getScoreFromAddress(address).getOwner().getAddress();
    }

    public Address getOrigin() {
        return getFirstFrame().from.getAddress();
    }

    public Address getCaller() {
        return getCurrentFrame().from.getAddress();
    }

    public Address getAddress() {
        return getCurrentFrame().to.getAddress();
    }

    private Score getScoreFromAddress(Address target) {
        var score = state.getScore(target);
        if (score == null) {
            throw new RevertedException("ContractNotFound(addr="+target+")");
        }
        return score;
    }

    public Object call(Account from, BigInteger value, Address targetAddress, String method, Object... params) {
        Score score = getScoreFromAddress(targetAddress);
        return score.call(from, false, value, method, params);
    }

    public Object call(BigInteger value, Address targetAddress, String method, Object... params) {
        Score from = getScoreFromAddress(getAddress());
        if ("fallback".equals(method) || "".equals(method)) {
            transfer(from.getAccount(), targetAddress, value);
            return null;
        } else {
            return call(from.getAccount(), value, true, false, targetAddress, method, params);
        }
    }

    Object call(Account from, BigInteger value, boolean transfer, boolean readonly, Address targetAddress, String method, Object[] params) {
        if (value.signum()<0) {
            throw new IllegalArgumentException("value is negative");
        }
        Score score = getScoreFromAddress(targetAddress);
        Account to = score.getAccount();
        pushFrame(from, to, readonly, method, value);
        try {
            if (value.signum()>0 && transfer) {
                from.subtractBalance(value);
                to.addBalance(value);
            }
            var obj = score.invokeMethod(method, params);
            applyFrame();
            return obj;
        } finally {
            popFrame();
        }
    }

    public void transfer(Account from, Address targetAddress, BigInteger value) {
        getBlock().increase();
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
                call(from, value, false, false, targetAddress, "fallback", new Object[]{});
            }
            state.apply();
        } finally {
            state.pop();
        }
    }

    public void putStorage(String key, Object value) {
        state.setValue(getAddress(), key, TypeConverter.toBytes(value));
    }

    public <T> T getStorage(Class<T> cls, String key) {
        return TypeConverter.fromBytes(cls, state.getValue(getAddress(), key));
    }

    public static class Block {
        private static Block sInstance;

        private long height;
        private long timestamp;

        private Block(long height, long timestamp) {
            this.height = height;
            this.timestamp = timestamp;
        }

        public static Block getInstance() {
            if (sInstance == null) {
                Random rand = new Random();
                sInstance = new Block(rand.nextInt(1000), System.nanoTime() / 1000);
            }
            return sInstance;
        }

        public long getHeight() {
            return height;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void increase() {
            increase(1);
        }

        public void increase(long delta) {
            height += delta;
            timestamp += 2_000_000 * delta; // 2 secs block generation
        }
    }

    public Block getBlock() {
        return Block.getInstance();
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
        var frame = getCurrentFrame();
        return frame!=null ? frame.isReadonly() : false;
    }

    protected void pushFrame(Account from, Account to, boolean readonly, String method, BigInteger value) {
        contexts.push(new Frame(from, to, isReadonly() || readonly, method, value));
        state.push();
    }

    protected void popFrame() {
        state.pop();
        contexts.pop();
    }

    protected void applyFrame() {
        state.apply();
    }

    public Frame getCurrentFrame() {
        return contexts.peek();
    }

    public Frame getFirstFrame() {
        return contexts.firstElement();
    }
}
