/*
 * Copyright 2023 PARAMETA Inc.
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

import com.iconloop.score.test.Event;
import com.iconloop.score.test.ServiceManager;
import org.junit.jupiter.api.Test;
import score.annotation.External;

import java.math.BigInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EventTest {
    private static Address addr1 = Address.fromString("cx1234000000000000000000000000000000000000");
    private static Address addr2 = Address.fromString("cx1235000000000000000000000000000000000000");
    @Test
    void testContructor() {
        assertDoesNotThrow(() -> {
            new Event(
                    addr1,
                    new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                    new Object[]{BigInteger.valueOf(1)}
            );
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Event(null, null, null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Event(addr1, null, null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Event(addr1, new Object[]{}, null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Event(
                    addr1,
                    new Object[]{"TestLog(int,int)", 1},
                    new Object[]{1}
            );
        });
    }
    @Test
    void testEquals() {
        var log1 = new Event(
                addr1,
                new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                new Object[]{BigInteger.valueOf(1)}
        );
        var log2 = new Event(
                addr1,
                new Object[]{"TestLog(int,int)", new byte[]{0x1}},
                new Object[]{new byte[]{0x1}}
        );
        assertEquals(log1, log2);
        assertEquals(log2, log1);

        var log3 = new Event(
                addr1,
                new Object[]{"TestLog(int,str)", new byte[]{0x1}},
                new Object[]{new byte[]{0x1}}
        );
        assertNotEquals(log2, log3);
        assertNotEquals(log3, log2);

        var log4 = new Event(
                addr2,
                new Object[]{"TestLog(int,str)", new byte[]{0x1}},
                new Object[]{new byte[]{0x1}}
        );
        assertNotEquals(log2, log4);
        assertNotEquals(log4, log2);

        var log5 = new Event(
                addr2,
                new Object[]{"TestLog(int,str)", null},
                new Object[]{new byte[]{0x1}}
        );
        var log5_2 = new Event(
                addr2,
                new Object[]{"TestLog(int,str)", null},
                new Object[]{new byte[]{0x1}}
        );
        assertEquals(log5, log5_2);
        assertEquals(log5_2, log5);

        assertNotEquals(log4, log5);
        assertNotEquals(log5, log4);

        var log6 = new Event(
                null,
                new Object[]{"TestLog(int,str)", null},
                new Object[]{new byte[]{0x1}}
        );
        var log6_2 = new Event(
                null,
                new Object[]{"TestLog(int,str)", null},
                new Object[]{new byte[]{0x1}}
        );
        assertEquals(log6, log6_2);
        assertEquals(log6_2, log6);

        assertNotEquals(log5, log6);
        assertNotEquals(log6, log5);
    }

    @Test
    void testMatch() {
        var log1 = new Event(
                addr1,
                new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                new Object[]{BigInteger.valueOf(1)}
        );
        var m1 = new Event(
                addr1,
                new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                new Object[]{null}
        );

        assertNotEquals(m1, log1);
        assertTrue(m1.match(log1));

        var m2 = new Event(
                null,
                new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                new Object[]{null}
        );

        assertNotEquals(m2, log1);
        assertTrue(m2.match(log1));

        var m2_2 = new Event(
                null,
                new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                new Object[]{null}
        );
        assertEquals(m2, m2_2);

        var m3 = new Event(
                addr2,
                new Object[]{"TestLog(int,int)", BigInteger.valueOf(1)},
                new Object[]{null}
        );

        assertNotEquals(m3, log1);
        assertFalse(m3.match(log1));
    }

    public static class Hello {
        @External
        public void hello(int v1, int v2) {
            Context.logEvent(
                    new Object[]{ "Hello(int,int)", BigInteger.valueOf(v1)},
                    new Object[]{ BigInteger.valueOf(v2)}
            );
        }

        @External
        public void helloAndRevert(int v1, int v2) {
            Context.logEvent(
                    new Object[]{ "Hello(int,int)", BigInteger.valueOf(v1)},
                    new Object[]{ BigInteger.valueOf(v2)}
            );
            Context.revert();
        }
    }

    public static class Caller {
        @External
        public void call(Address addr, String method, int v1, int v2) {
            try {
                Context.call(addr, method, v1, v2);
            } catch (score.RevertedException e) {
                Context.logEvent(
                        new Object[]{"Reverted()"},
                        new Object[]{}
                );
            }
        }
    }

    @Test
    public void testRevert() throws Exception {
        var sm = ServiceManager.getInstance();
        var acc1 = sm.createAccount();
        var hello_score = sm.deploy(acc1, Hello.class);

        {
            hello_score.invoke(acc1, "hello", 2, 3);
            var logs = sm.getLastEventLogs();
            assertTrue(logs.contains(new Event(
                    hello_score.getAddress(),
                    new Object[]{"Hello(int,int)", BigInteger.valueOf(2)},
                    new Object[]{BigInteger.valueOf(3)}
            )));
        }

        {
            assertThrows(UserRevertedException.class, () -> {
                hello_score.invoke(acc1, "helloAndRevert", 4, 5);
            });
            var logs = sm.getLastEventLogs();
            assertFalse(logs.contains(new Event(
                    hello_score.getAddress(),
                    new Object[]{"Hello(int,int)", BigInteger.valueOf(4)},
                    new Object[]{BigInteger.valueOf(5)}
            )));
        }

        var caller_score = sm.deploy(acc1, Caller.class);

        {
            caller_score.invoke(acc1, "call",
                    hello_score.getAddress(), "hello", 6, 7
            );
            var logs = sm.getLastEventLogs();
            assertTrue(logs.contains(new Event(
                    hello_score.getAddress(),
                    new Object[]{"Hello(int,int)", BigInteger.valueOf(6)},
                    new Object[]{BigInteger.valueOf(7)}
            )));
            assertFalse(logs.contains(new Event(
                    caller_score.getAddress(),
                    new Object[]{"Reverted()"},
                    new Object[]{}
            )));
        }

        {
            caller_score.invoke(acc1, "call",
                    hello_score.getAddress(), "helloAndRevert", 6, 7
            );
            var logs = sm.getLastEventLogs();
            assertFalse(logs.contains(new Event(
                    hello_score.getAddress(),
                    new Object[]{"Hello(int,int)", BigInteger.valueOf(6)},
                    new Object[]{BigInteger.valueOf(7)}
            )));
            assertTrue(logs.contains(new Event(
                    caller_score.getAddress(),
                    new Object[]{"Reverted()"},
                    new Object[]{}
            )));
        }
    }
}
