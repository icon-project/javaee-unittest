/*
 * Copyright 2023 PARAMETA Corp.
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
import com.iconloop.score.test.GenerateTScore;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TScoreTest {

    private static final ServiceManager sm = ServiceManager.getInstance();

    public static class DataOnlyContract {
        @External
        public void emit(String arg1, short arg2, int arg3, long arg4) {
            Context.println("arg1:" + arg1 + ",arg2:" + arg2 +
                    ",arg3:" + arg3 + ",arg4:" + arg4);
            DataOnly(arg1, arg2, arg3, arg4);
        }

        @EventLog
        public void DataOnly(String arg1, short arg2, int arg3, long arg4) {
        }
    }

    public static class IndexedOnlyContract {
        @External
        public void emit(byte[] arg1, byte arg2, char arg3, boolean arg4) {
            Context.println("arg1:" + new String(arg1) + ",arg2:" + arg2 +
                    ",arg3:" + arg3 + ",arg4:" + arg4);
            IndexedOnly(arg1, arg2, arg3, arg4);
        }

        @EventLog(indexed = 4)
        public void IndexedOnly(byte[] arg1, byte arg2, char arg3, boolean arg4) {
        }
    }

    @GenerateTScore
    public static class Contract {

        private final VarDB<String> varDB = Context.newVarDB("value", String.class);

        public Contract(String value) {
            if (varDB.get() == null) {
                varDB.set(value);
            } else {
                Context.println("ignore constructor argument");
            }
        }

        @External(readonly = true)
        public String readonlyMethod() {
            return varDB.get();
        }

        @External
        public void writableMethod(@Optional String value) {
            varDB.set(value);
        }

        @External
        public void writableMethod2(String value) {
            varDB.set(value);
        }

        public void writableMethodInternal(String value) {
            varDB.set(value);
        }

        @Payable
        @External
        public void payableMethod() {
            EventMethod(Context.getCaller(), Context.getValue());
        }

        @EventLog(indexed = 1)
        public void EventMethod(Address caller, BigInteger value) {
        }
    }

    private static final Account owner = sm.createAccount();
    private static Score dataOnlyScore;
    private static TScoreTestIndexedOnlyContractTScore.Client indexedOnlyClient;

    @GenerateTScore(DataOnlyContract.class)
    @GenerateTScore(value = IndexedOnlyContract.class, suffix = "TScore")
    @BeforeAll
    static void setup() throws Exception {
        dataOnlyScore = sm.deploy(owner, TScoreTestDataOnlyContractTS.class);
        indexedOnlyClient = new TScoreTestIndexedOnlyContractTScore.Client(
                sm.deploy(owner, TScoreTestIndexedOnlyContractTScore.class));
    }

    @Test
    public void emitDataOnly() {
        String arg1 = "stringValue";
        short arg2 = 0x2;
        int arg3 = 0x3;
        long arg4 = 0x4;
        dataOnlyScore.invoke(owner, "emit", arg1, arg2, arg3, arg4);
        var logs = sm.getLastEventLogs();
        assertTrue(logs.contains(new Event(
                dataOnlyScore.getAddress(),
                new Object[]{"DataOnly(str,int,int,int)"},
                new Object[]{arg1, BigInteger.valueOf(arg2),
                        BigInteger.valueOf(arg3),BigInteger.valueOf(arg4)})));
    }

    @Test
    public void emitIndexedOnly() {
        byte[] arg1 = "bytesValue".getBytes();
        byte arg2 = 0x2;
        char arg3 = '3';
        boolean arg4 = true;
        indexedOnlyClient.emit(arg1, arg2, arg3, arg4);
        var logs = sm.getLastEventLogs();
        assertTrue(logs.contains(
                indexedOnlyClient.IndexedOnly(arg1, arg2, arg3, arg4)));
    }

    @Test
    public void contractTest() throws Exception {
        String constructorArg = "init";
        TScoreTestContractTS.Client client = TScoreTestContractTS.Client.deploy(sm, owner, constructorArg);
        assertEquals(constructorArg, client.readonlyMethod());

        String arg = "value";
        client.writableMethod(arg);
        assertEquals(arg, client.readonlyMethod());

        // invoke skipping optional
        client.writableMethod(null);
        assertNull(client.readonlyMethod());

        // invoke with fewer parameters
        assertThrows(IllegalArgumentException.class, ()-> {
            client.score().invoke(owner, "writableMethod2");
        });

        String arg2 = "value";
        client.writableMethod2(arg2);
        assertEquals(arg2, client.readonlyMethod());

        Account caller = sm.createAccount(10);
        BigInteger value = BigInteger.ONE;
        client.from(caller).payableMethod(value);

        var logs = sm.getLastEventLogs();
        Event expectedEvent = client.EventMethod(caller.getAddress(), value);
        assertTrue(logs.contains(expectedEvent));

        // calling non-payable with a value
        assertThrows(IllegalArgumentException.class, ()-> {
            client.score().invoke(owner, value, "writableMethod2", arg2);
        });

        // accessing non-external method
        assertThrows(IllegalArgumentException.class, ()-> {
            client.score().invoke(owner, "writableMethodInternal", arg2);
        });
    }
}
