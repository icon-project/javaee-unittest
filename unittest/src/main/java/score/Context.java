/*
 * Copyright 2023 PARAMETA Corp.
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

import com.iconloop.score.test.ManualRevertException;
import com.iconloop.score.test.TestBase;
import foundation.icon.ee.io.RLPDataReader;
import foundation.icon.ee.io.RLPDataWriter;
import foundation.icon.ee.io.RLPNDataReader;
import foundation.icon.ee.io.RLPNDataWriter;
import score.impl.AnyDBImpl;
import score.impl.Crypto;
import score.impl.ObjectReaderImpl;
import score.impl.ObjectWriterImpl;
import score.impl.TypeConverter;

import java.math.BigInteger;

public final class Context extends TestBase {
    private static final ServiceManagerImpl sm = ServiceManagerImpl.getServiceManagerImpl();

    private Context() {
    }

    public static byte[] getTransactionHash() {
        return sm.getTransactionHash();
    }

    public static int getTransactionIndex() {
        return sm.getTransactionIndex();
    }

    public static long getTransactionTimestamp() {
        return sm.getTransactionTimestamp();
    }

    public static BigInteger getTransactionNonce() {
        return BigInteger.ZERO;
    }

    public static Address getAddress() {
        return sm.getAddress();
    }

    public static Address getCaller() {
        return sm.getCaller();
    }

    public static Address getOrigin() {
        return sm.getOrigin();
    }

    public static Address getOwner() {
        return sm.getOwner();
    }

    public static BigInteger getValue() {
        return sm.getCurrentFrame().getValue();
    }

    public static long getBlockTimestamp() {
        return sm.getBlock().getTimestamp();
    }

    public static long getBlockHeight() {
        return sm.getBlock().getHeight();
    }

    public static BigInteger getBalance(Address address) throws IllegalArgumentException {
        return sm.getAccount(address).getBalance();
    }

    public static<T> T call(Class<T> cls, BigInteger value,
                            Address targetAddress, String method, Object... params) {
        return TypeConverter.cast(sm.call(value, targetAddress, method, params), cls);
    }

    public static Object call(BigInteger value,
                              Address targetAddress, String method, Object... params) {
        return TypeConverter.cast(sm.call(value, targetAddress, method, params));
    }

    public static<T> T call(Class<T> cls,
                            Address targetAddress, String method, Object... params) {
        return TypeConverter.cast(sm.call(BigInteger.ZERO, targetAddress, method, params), cls);
    }

    public static Object call(Address targetAddress, String method, Object... params) {
        return TypeConverter.cast(sm.call(BigInteger.ZERO, targetAddress, method, params));
    }

    public static void transfer(Address targetAddress, BigInteger value) {
        sm.call(value, targetAddress, "fallback");
    }

    public static Address deploy(byte[] content, Object... params) {
        return null;
    }

    public static Address deploy(Address targetAddress, byte[] content, Object... params) {
        return null;
    }

    public static void revert(int code, String message) {
        throw new ManualRevertException(code, String.format("Reverted(%d): %s", code, message));
    }

    public static void revert(int code) {
        throw new ManualRevertException(code, String.format("Reverted(%d)", code));
    }

    public static void revert(String message) {
        revert(0, message);
    }

    public static void revert() {
        revert(0);
    }

    public static void require(boolean condition) {
        if (!condition) {
            revert();
        }
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            revert(message);
        }
    }

    public static void println(String message) {
        System.out.println(message);
    }

    public static byte[] hash(String alg, byte[] msg) {
        require(null != alg, "Algorithm can't be NULL");
        require(null != msg, "Message can't be NULL");
        return Crypto.hash(alg, msg);
    }

    public static boolean verifySignature(String alg, byte[] msg, byte[] sig, byte[] pubKey) {
        require(null != alg, "Algorithm can't be NULL");
        require(null != msg, "Message can't be NULL");
        require(null != sig, "Signature can't be NULL");
        require(null != pubKey, "Public key can't be NULL");
        return Crypto.verifySignature(alg, msg, sig, pubKey);
    }

    public static byte[] recoverKey(String alg, byte[] msg, byte[] sig, boolean compressed) {
        require(null != msg && null != sig);
        require(msg.length == 32, "the length of msg must be 32");
        require(sig.length == 65, "the length of sig must be 65");
        return Crypto.recoverKey(alg, msg, sig, compressed);
    }

    public static byte[] aggregate(String type, byte[] prevAgg, byte[] values) {
        require(null != type, "Type can't be NULL");
        require(null != values, "Values can't be NULL");
        return Crypto.aggregate(type, prevAgg, values);
    }

    public static byte[] ecAdd(String curve, byte[] data, boolean compressed) {
        require(null != curve, "Elliptic curve can't be NULL");
        require(null != data, "Data can't be NULL");
        switch (curve) {
            case "bls12-381-g1":
                return Crypto.bls12381G1Add(data, compressed);
            case "bls12-381-g2":
                return Crypto.bls12381G2Add(data, compressed);
        }
        throw new IllegalArgumentException("Unsupported curve " + curve);
    }

    public static byte[] ecScalarMul(String curve, byte[] scalar, byte[] data, boolean compressed) {
        require(null != curve, "Elliptic curve can't be NULL");
        require(null != scalar, "Scalar can't be NULL");
        require(null != data, "Data can't be NULL");
        switch (curve) {
            case "bls12-381-g1":
                return Crypto.bls12381G1ScalarMul(scalar, data, compressed);
            case "bls12-381-g2":
                return Crypto.bls12381G2ScalarMul(scalar, data, compressed);
        }
        throw new IllegalArgumentException("Unsupported curve " + curve);
    }

    public static boolean ecPairingCheck(String curve, byte[] data, boolean compressed) {
        require(null != curve, "Elliptic curve can't be NULL");
        require(null != data, "Data can't be NULL");
        switch (curve) {
            case "bls12-381":
                return Crypto.bls12381PairingCheck(data, compressed);
        }
        throw new IllegalArgumentException("Unsupported curve " + curve);
    }

    public static Address getAddressFromKey(byte[] pubKey) {
        require(null != pubKey, "pubKey can't be NULL");
        return new Address(Crypto.getAddressBytesFromKey(pubKey));
    }

    public static int getFeeSharingProportion() {
        return 0;
    }

    public static void setFeeSharingProportion(int proportion) {
    }

    @SuppressWarnings("unchecked")
    public static<K, V> BranchDB<K, V> newBranchDB(String id, Class<?> leafValueClass) {
        return AnyDBImpl.newBranchDB(sm, id, leafValueClass);
    }

    @SuppressWarnings("unchecked")
    public static<K, V> DictDB<K, V> newDictDB(String id, Class<V> valueClass) {
        return AnyDBImpl.newDictDB(sm, id, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static<E> ArrayDB<E> newArrayDB(String id, Class<E> valueClass) {
        return AnyDBImpl.newArrayDB(sm, id, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static<E> VarDB<E> newVarDB(String id, Class<E> valueClass) {
        return AnyDBImpl.newVarDB(sm, id, valueClass);
    }

    public static void logEvent(Object[] indexed, Object[] data) {
        sm.logEvent(indexed, data);
    }

    public static ObjectReader newByteArrayObjectReader(String codec, byte[] byteArray) {
        if ("RLPn".equals(codec)) {
            return new ObjectReaderImpl(new RLPNDataReader(byteArray));
        } else if ("RLP".equals(codec)) {
            return new ObjectReaderImpl(new RLPDataReader(byteArray));
        }
        throw new IllegalArgumentException("Unknown codec");
    }

    public static ByteArrayObjectWriter newByteArrayObjectWriter(String codec) {
        if ("RLPn".equals(codec)) {
            return new ObjectWriterImpl(new RLPNDataWriter());
        } else if ("RLP".equals(codec)) {
            return new ObjectWriterImpl(new RLPDataWriter());
        }
        throw new IllegalArgumentException("Unknown codec");
    }
}
