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
import com.iconloop.score.test.OutOfBalanceException;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.annotation.External;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static final int ERROR_CODE = 100;

    public static class Callee {
        @External
        public void expectRevert() {
            Context.revert(ERROR_CODE);
        }

        @External
        public void expectRevert2() {
            try {
                Context.revert(ERROR_CODE);
                // must catch nothing
            } catch (RevertedException e) {
            }
        }

        static public class MyRevertException extends UserRevertException {
            public MyRevertException() {
                super();
            }

            @Override
            public int getCode() {
                return ERROR_CODE;
            }
        }

        @External
        public void expectRevert3() {
            throw new MyRevertException();
        }

        @External
        public void expectNoRevert() {
            try {
                this.expectRevert3();
            } catch (UserRevertException ure) {
                Context.require(ure.getCode() == ERROR_CODE);
            }
        }

        @External
        public void expectNoRevertWithString(String ignore) {
            // do nothing
        }
    }

    public static class Caller {
        @External
        public void invoke(Address callee) {
            try {
                Context.call(callee, "expectRevert");
            } catch (UserRevertedException e) {
                if (e.getCode() != ERROR_CODE) {
                    Context.revert(0);
                }
            }
        }

        @External
        public void invokeNoCatch(Address callee, BigInteger value, String method) {
            Context.call(value, callee, method);
        }
    }

    private static Score caller;
    private static Score callee;

    @BeforeAll
    static void setup() throws Exception {
        caller = sm.deploy(owner, Caller.class);
        callee = sm.deploy(owner, Callee.class);
    }

    @Test
    void testUserRevert() {
        // Context.revert is mapped to UserRevertedException
        var ure = assertThrows(UserRevertedException.class, () ->
                callee.invoke(owner, "expectRevert"));
        assertEquals(ERROR_CODE, ure.getCode());

        // Context.revert is not catchable with RevertedException
        var ure2 = assertThrows(UserRevertedException.class, () ->
                callee.invoke(owner, "expectRevert2"));
        assertEquals(ERROR_CODE, ure2.getCode());

        // MyRevertException is mapped to UserRevertedException
        var ure3 = assertThrows(UserRevertedException.class, () ->
                callee.invoke(owner, "expectRevert3"));
        assertEquals(ERROR_CODE, ure3.getCode());

        // Catch MyRevertException between internal calls
        assertDoesNotThrow(() -> callee.invoke(owner, "expectNoRevert"));

        // Catch UserRevertedException from inter-call
        assertDoesNotThrow(() -> caller.invoke(owner, "invoke", callee.getAddress()));
    }

    @Test
    void testBasicExceptions() {
        // Invalid method name result in IllegalArgumentException
        assertThrows(IllegalArgumentException.class, ()-> {
            sm.invoke(owner, BigInteger.ZERO, caller.getAddress(), "invoke2");
        });

        // Invalid address result in IllegalArgumentException
        var unknownAddr = sm.createScoreAccount().getAddress();
        assertThrows(IllegalArgumentException.class, ()-> {
            sm.invoke(owner, BigInteger.ZERO, unknownAddr, "test");
        });

        // Invalid parameter result in IllegalArgumentException
        assertThrows(IllegalArgumentException.class, ()-> {
            sm.invoke(owner, BigInteger.ZERO, callee.getAddress(), "expectNoRevertWithString", 3);
        });

        // Call with Low balance result in OutOfBalanceException
        assertThrows(OutOfBalanceException.class, () -> callee.invoke(owner, BigInteger.TEN, "expectNoRevertWithString", "VALID"));

        // Transfer with Low balance result in OutOfBalanceException
        assertThrows(OutOfBalanceException.class, () -> sm.transfer(owner, callee.getAddress(), BigInteger.TEN));
    }

    @Test
    void testMappingException() {
        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "invokeNoCatch",
                        callee.getAddress(), BigInteger.ZERO, "invalidMethod")
        );

        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "invokeNoCatch",
                        callee.getAddress(), BigInteger.ZERO, "expectRevert")
        );

        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "invokeNoCatch",
                        callee.getAddress(), BigInteger.ZERO, "expectRevert2")
        );

        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "invokeNoCatch",
                        callee.getAddress(), BigInteger.ZERO, "expectRevert3")
        );

        assertThrows(RevertedException.class, () ->
                caller.invoke(owner, "invokeNoCatch",
                        callee.getAddress(), BigInteger.TEN, "expectNoRevert")
        );
    }
}
