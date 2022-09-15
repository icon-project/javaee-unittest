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
import score.annotation.Optional;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CallTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score echoScore;

    public static class Echo {
        public int nonExternal() {
            return 0;
        }

        @External(readonly=true)
        public String echo(@Optional String message) {
            return message;
        }

        @External(readonly=true)
        public Address echoAddress(@Optional Address _addr) {
            return _addr;
        }

        @External(readonly=true)
        public byte[] echoByteArray(@Optional byte[] _data) {
            return _data;
        }

        @External(readonly=true)
        public boolean echoBoolean(@Optional boolean bool) {
            return bool;
        }

        @External(readonly=true)
        public byte echoByte(@Optional byte _byte) {
            return _byte;
        }

        @External(readonly=true)
        public char echoChar(@Optional char _char) {
            return _char;
        }

        @External(readonly=true)
        public BigInteger echoBigInteger(@Optional BigInteger bigInteger) {
            return bigInteger;
        }

        @External(readonly=true)
        public int echoInteger(@Optional int _int) {
            return _int;
        }

        @External(readonly=true)
        public long echoLong(@Optional long _long) {
            return _long;
        }

        @External(readonly=true)
        public short echoShort(@Optional short _short) {
            return _short;
        }

        @External(readonly=true)
        public String echoFirst(String[] messages) {
            return messages[0];
        }

        @External(readonly=true)
        public String castedEcho(String message) {
            return (String) Context.call(Context.getAddress(), "echo", message);
        }

        @External(readonly=true)
        public String typedEcho(String message) {
            return Context.call(String.class, Context.getAddress(), "echo", message);
        }

        @External(readonly=true)
        public String listEcho() {
            List<String> list = List.of("test1", "test2", "test3");
            return Context.call(String.class, Context.getAddress(), "echoFirst", list);
        }

        @External(readonly=true)
        public String arrayListEcho() {
            List<String> list = new ArrayList<>();
            list.add("test1");
            list.add("test2");
            list.add("test3");
            return Context.call(String.class, Context.getAddress(), "echoFirst", list);
        }
    }

    @BeforeAll
    static void setUp() throws Exception {
        echoScore = sm.deploy(owner, Echo.class);
    }

    @Test
    void callNonExternal() {
        assertThrows(RuntimeException.class, () ->
                echoScore.call("nonExternal"));
    }

    @Test
    void callCasted() {
        String echoMessage = "test";
        assertEquals(echoMessage, echoScore.call("castedEcho", echoMessage));
    }

    @Test
    void callTyped() {
        String echoMessage = "test";
        assertEquals(echoMessage, echoScore.call("typedEcho", echoMessage));
    }

    @Test
    void parameterConversions_array() {
        String echoMessage = "test1";
        assertEquals(echoMessage, echoScore.call("listEcho"));
        assertEquals(echoMessage, echoScore.call("arrayListEcho"));
    }

    @Test
    void parameterConversions_Optional() {
        assertNull(echoScore.call("echo"));
        assertNull(echoScore.call("echoAddress"));
        assertNull(echoScore.call("echoByteArray"));
        assertEquals(BigInteger.ZERO, echoScore.call("echoBigInteger"));
        assertEquals(BigInteger.ONE, echoScore.call("echoBigInteger", BigInteger.ONE));

        assertEquals(0, echoScore.call("echoInteger"));
        assertEquals(Short.valueOf("0"), echoScore.call("echoShort"));
        assertEquals(Long.valueOf("0"), echoScore.call("echoLong"));
        assertEquals(Character.MIN_VALUE, echoScore.call("echoChar"));
        assertEquals(Byte.valueOf("0"), echoScore.call("echoByte"));
        assertEquals(Boolean.FALSE, echoScore.call("echoBoolean"));
    }
}
