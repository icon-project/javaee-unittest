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
    }

    private static Score callee;
    private static Score callee2;
    private static Score caller;
    private static Account user1 = sm.createAccount();

    @BeforeAll
    static void setup() throws Exception {
        callee = sm.deploy(owner, Callee.class);
        callee2 = sm.deploy(owner, CalleeWithoutFallback.class);
        caller = sm.deploy(owner, Caller.class);
    }

    @Test
    void testProxyCall() throws Exception {
        assertDoesNotThrow(() ->
                caller.invoke(owner, "proxyCall", callee.getAddress())
        );
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
