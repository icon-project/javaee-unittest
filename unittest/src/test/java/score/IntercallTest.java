/*
 * Copyright 2022 ICONLOOP Inc.
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
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IntercallTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();

    public static class Callee {
        VarDB<BigInteger> store = Context.newVarDB("store", BigInteger.class);

        public Callee() { }
        @External(readonly=true)
        public String getName() {
            return Callee.class.toString();
        }

        @Payable
        public void fallback() {
            store.set(store.getOrDefault(BigInteger.ZERO).add(Context.getValue()));
        }

        @External
        @Payable
        public void deposit() {
            store.set(store.getOrDefault(BigInteger.ZERO).add(Context.getValue()));
        }

        @External(readonly = true)
        public BigInteger getStored() {
            return store.getOrDefault(BigInteger.ZERO);
        }

        @External(readonly=true)
        public boolean[] getBoolArray(boolean[] v) {
            return v;
        }

        @External(readonly=true)
        public int[] getIntArray(int[] v) {
            return v;
        }

        @External(readonly=true)
        public String[] getStrArray(String[] v) {
            return v;
        }

        @External(readonly=true)
        public byte[][] getBytesArray(byte[][] v) {
            return v;
        }

        @External(readonly=true)
        public Address[] getAddressArray(Address[] v) {
            return v;
        }

        @External(readonly=true)
        public BigInteger[] getBigIntArray(BigInteger[] v) {
            return v;
        }
    }

    public static class CalleeWithoutFallback {
        @External(readonly=true)
        public String getName() {
            return Callee.class.toString();
        }
    }

    public static class Caller {
        @External
        public void proxyCall(Address callee) {
            var proxy = new Proxy(callee);
            Context.println("name: " + proxy.getName());
        }

        private static class Proxy {
            private final Address target;

            public Proxy(Address target) {
                this.target = target;
            }

            public String getName() {
                 return Context.call(String.class, target, "getName");
            }
        }

        @External
        public void invoke(Address target, BigInteger value, String method) {
            Context.call(value, target, method);
        }

        @External
        public void transfer(Address target, BigInteger value) {
            Context.transfer(target, value);
        }

        @External
        public void callWithType(Address target, String type) {
            switch (type) {
                case "bool": {
                    var ba = new boolean[]{false, true};
                    var ret = (List<Object>) Context.call(target, "getBoolArray", (Object) ba);
                    Context.require(ret.size() == ba.length);
                    for (int i = 0; i < ba.length; i++) {
                        var e = ret.get(i);
                        if (e instanceof Boolean) {
                            Context.require(((Boolean) e) == ba[i]);
                        } else {
                            Context.revert("not a boolean");
                        }
                    }
                    break;
                }
                case "int": {
                    var ia = new int[]{1, 2, 3};
                    var ret = (List<Object>) Context.call(target, "getIntArray", (Object) ia);
                    Context.require(ret.size() == ia.length);
                    for (int i = 0; i < ia.length; i++) {
                        var e = ret.get(i);
                        if (e instanceof BigInteger) {
                            Context.require(((BigInteger) e).intValue() == ia[i]);
                        } else {
                            Context.revert("not a BigInteger");
                        }
                    }
                    break;
                }
                case "str": {
                    var sa = new String[]{"a", "b", "c"};
                    var ret = (List<Object>) Context.call(target, "getStrArray", (Object) sa);
                    Context.require(ret.size() == sa.length);
                    for (int i = 0; i < sa.length; i++) {
                        var e = ret.get(i);
                        if (e instanceof String) {
                            Context.require(e.equals(sa[i]));
                        } else {
                            Context.revert("not a String");
                        }
                    }
                    break;
                }
                case "bytes": {
                    var ba = new byte[][]{new byte[]{0x1, 0x2}, new byte[]{0x3, 0x4}};
                    var ret = (List<Object>) Context.call(target, "getBytesArray", (Object) ba);
                    Context.require(ret.size() == ba.length);
                    for (int i = 0; i < ba.length; i++) {
                        var e = ret.get(i);
                        if (e instanceof byte[]) {
                            Context.require(Arrays.equals((byte[]) e, ba[i]));
                        } else {
                            Context.revert("not a byte[]");
                        }
                    }
                    break;
                }
                case "Address": {
                    var aa = new Address[]{
                            Address.fromString("hx0000000000000000000000000000000000000001"),
                            Address.fromString("hx0000000000000000000000000000000000000002")
                    };
                    var ret = (List<Object>) Context.call(target, "getAddressArray", (Object) aa);
                    Context.require(ret.size() == aa.length);
                    for (int i = 0; i < aa.length; i++) {
                        var e = ret.get(i);
                        if (e instanceof Address) {
                            Context.require(e.equals(aa[i]));
                        } else {
                            Context.revert("not an Address");
                        }
                    }
                    break;
                }
                case "bigInt": {
                    var ba = new BigInteger[]{BigInteger.ZERO, BigInteger.ONE, BigInteger.TWO};
                    var ret = (List<Object>) Context.call(target, "getBigIntArray", (Object) ba);
                    Context.require(ret.size() == ba.length);
                    for (int i = 0; i < ba.length; i++) {
                        var e = ret.get(i);
                        if (e instanceof BigInteger) {
                            Context.require(e.equals(ba[i]));
                        } else {
                            Context.revert("not a BigInteger");
                        }
                    }
                    break;
                }
                case "boolList": {
                    var bl = List.of(false, true);
                    var ret = (List<Object>) Context.call(target, "getBoolArray", (Object) bl);
                    Context.require(ret.size() == bl.size());
                    for (int i = 0; i < bl.size(); i++) {
                        var e = ret.get(i);
                        if (e instanceof Boolean) {
                            Context.require(e == bl.get(i));
                        } else {
                            Context.revert("not a boolean");
                        }
                    }
                    break;
                }
                case "intList": {
                    var il = List.of(1, 2, 3);
                    var ret = (List<Object>) Context.call(target, "getIntArray", (Object) il);
                    Context.require(ret.size() == il.size());
                    for (int i = 0; i < il.size(); i++) {
                        var e = ret.get(i);
                        if (e instanceof BigInteger) {
                            Context.require(((BigInteger) e).intValue() == il.get(i));
                        } else {
                            Context.revert("not a BigInteger");
                        }
                    }
                    break;
                }
                case "strList": {
                    var sl = List.of("a", "b", "c");
                    var ret = (List<Object>) Context.call(target, "getStrArray", (Object) sl);
                    Context.require(ret.size() == sl.size());
                    for (int i = 0; i < sl.size(); i++) {
                        var e = ret.get(i);
                        if (e instanceof String) {
                            Context.require(e.equals(sl.get(i)));
                        } else {
                            Context.revert("not a String");
                        }
                    }
                    break;
                }
                case "bytesList": {
                    var bl = List.of(new byte[]{0x1, 0x2}, new byte[]{0x3, 0x4});
                    var ret = (List<Object>) Context.call(target, "getBytesArray", (Object) bl);
                    Context.require(ret.size() == bl.size());
                    for (int i = 0; i < bl.size(); i++) {
                        var e = ret.get(i);
                        if (e instanceof byte[]) {
                            Context.require(Arrays.equals((byte[]) e, bl.get(i)));
                        } else {
                            Context.revert("not a byte[]");
                        }
                    }
                    break;
                }
                case "AddressList": {
                    var aa = List.of(
                            Address.fromString("hx0000000000000000000000000000000000000001"),
                            Address.fromString("hx0000000000000000000000000000000000000002")
                    );
                    var ret = (List<Object>) Context.call(target, "getAddressArray", (Object) aa);
                    Context.require(ret.size() == aa.size());
                    for (int i = 0; i < aa.size(); i++) {
                        var e = ret.get(i);
                        if (e instanceof Address) {
                            Context.require(e.equals(aa.get(i)));
                        } else {
                            Context.revert("not an Address");
                        }
                    }
                    break;
                }
                default:
                    Context.revert("Unknown type: " + type);
            }
        }
    }

    private static Score callee;
    private static Score callee2;
    private static Score caller;
    private static final Account user1 = sm.createAccount();

    @BeforeAll
    static void setup() throws Exception {
        callee = sm.deploy(owner, Callee.class);
        callee2 = sm.deploy(owner, CalleeWithoutFallback.class);
        caller = sm.deploy(owner, Caller.class);
    }

    @Test
    void testProxyCall() {
        assertDoesNotThrow(() ->
                caller.invoke(owner, "proxyCall", callee.getAddress())
        );
    }

    @Test
    void testTypeConversion() {
        String[] tests = {
                "bool", "int", "str", "bytes", "Address", "bigInt",
                "boolList", "intList", "strList", "bytesList", "AddressList"};
        for (var type : tests) {
            assertDoesNotThrow(() ->
                    caller.invoke(owner, "callWithType", callee.getAddress(), type));
        }
    }

    @Test
    void testTransfer() {
        BigInteger balance;

        // Transfer to the EoA through Context.transfer without budget (expecting failure)
        balance = user1.getBalance();
        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "transfer", user1.getAddress(), BigInteger.TEN));
        assertEquals(BigInteger.ZERO, user1.getBalance().subtract(balance));

        // Transfer to the EoA through Context.transfer
        balance = user1.getBalance();
        caller.getAccount().addBalance(BigInteger.TEN);
        assertDoesNotThrow(() ->
                caller.invoke(owner, "transfer", user1.getAddress(), BigInteger.TEN));
        assertEquals(BigInteger.TEN, user1.getBalance().subtract(balance));

        // Transfer to the EoA through Context.call
        balance = user1.getBalance();
        caller.getAccount().addBalance(BigInteger.TEN);
        assertDoesNotThrow(() ->
                caller.invoke(owner, "invoke", user1.getAddress(), BigInteger.TEN, "anyMethod", "anyParam"));
        assertEquals(BigInteger.TEN, user1.getBalance().subtract(balance));

        // Transfer to the contract with fallback method through Context.transfer
        balance = callee.getAccount().getBalance();
        caller.getAccount().addBalance(BigInteger.TEN);
        assertDoesNotThrow(() ->
                caller.invoke(owner, "transfer", callee.getAddress(), BigInteger.TEN));
        assertEquals(BigInteger.TEN, callee.getAccount().getBalance().subtract(balance));

        // Transfer to the contract without fallback through Context.transfer (expecting failure)
        balance = callee2.getAccount().getBalance();
        caller.getAccount().addBalance(BigInteger.TEN);
        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "transfer", callee2.getAddress(), BigInteger.TEN));
        assertEquals(BigInteger.ZERO, callee2.getAccount().getBalance().subtract(balance));
        caller.getAccount().subtractBalance(BigInteger.TEN);

        // Transfer to the contract with method through Context.call
        balance = callee.getAccount().getBalance();
        caller.getAccount().addBalance(BigInteger.TEN);
        assertDoesNotThrow(() ->
                caller.invoke(owner, "invoke", callee.getAddress(), BigInteger.TEN, "deposit"));
        assertEquals(BigInteger.TEN, callee.getAccount().getBalance().subtract(balance));
        assertEquals(callee.getAccount().getBalance(), callee.call("getStored"));
    }
}
