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

import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Context extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final StackWalker stackWalker =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private Context() {
    }

    public static byte[] getTransactionHash() {
        return null;
    }

    public static int getTransactionIndex() {
        return 0;
    }

    public static long getTransactionTimestamp() {
        return 0L;
    }

    public static BigInteger getTransactionNonce() {
        return BigInteger.ZERO;
    }

    public static Address getAddress() {
        var caller = stackWalker.getCallerClass();
        return sm.getAddress(caller);
    }

    public static Address getCaller() {
        var caller = stackWalker.getCallerClass();
        return sm.getCaller(caller);
    }

    public static Address getOrigin() {
        var caller = stackWalker.getCallerClass();
        return sm.getOrigin(caller);
    }

    public static Address getOwner() {
        var caller = stackWalker.getCallerClass();
        return sm.getOwner(caller);
    }

    public static BigInteger getValue() {
        return BigInteger.ZERO;
    }

    public static long getBlockTimestamp() {
        return 0L;
    }

    public static long getBlockHeight() {
        return 0L;
    }

    public static BigInteger getBalance(Address address) throws IllegalArgumentException {
        return BigInteger.ZERO;
    }

    public static Object call(BigInteger value, Address targetAddress, String method, Object... params) {
        return null;
    }

    public static Object call(Address targetAddress, String method, Object... params) {
        return null;
    }

    public static void transfer(Address targetAddress, BigInteger value) {
    }

    public static void revert(int code, String message) {
    }

    public static void revert(int code) {
    }

    public static void revert(String message) {
    }

    public static void revert() {
    }

    public static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    public static void println(String message) {
        System.out.println(message);
    }

    public static byte[] sha3_256(byte[] data) throws IllegalArgumentException {
        return null;
    }

    public static byte[] recoverKey(byte[] msgHash, byte[] signature, boolean compressed) {
        return null;
    }

    public static Address getAddressFromKey(byte[] publicKey) {
        return null;
    }

    public static<K, V> BranchDB<K, V> newBranchDB(String id, Class<?> leafValueClass) {
        return null;
    }

    public static<K, V> DictDB<K, V> newDictDB(String id, Class<V> valueClass) {
        return new StubDictDB<>(id, valueClass);
    }

    public static<E> ArrayDB<E> newArrayDB(String id, Class<E> valueClass) {
        return null;
    }

    public static<E> VarDB<E> newVarDB(String id, Class<E> valueClass) {
        return null;
    }

    public static void logEvent(Object[] indexed, Object[] data) {
    }

    private static class StubDictDB<K, V> implements DictDB<K, V> {
        private final Map<K, V> map = new HashMap<>();

        public StubDictDB(String id, Class<V> valueClass) {
        }

        @Override
        public void set(K k, V v) {
            if (sm.getCurrentContext()) {
                throw new IllegalStateException("read-only context");
            }
            map.put(k, v);
        }

        @Override
        public V get(K k) {
            return map.get(k);
        }

        @Override
        public V getOrDefault(K k, V v) {
            return map.getOrDefault(k, v);
        }
    }
}
