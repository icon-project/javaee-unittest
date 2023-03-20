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

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Event;
import com.iconloop.score.test.EventImplement;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.annotation.EventLog;
import score.annotation.External;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventImplementTest {

    private static ServiceManager sm = ServiceManager.getInstance();

    @EventImplement
    public static class DataOnlyContract {
        @External
        public void emit(String arg1, String arg2) {
            Context.println("arg1:"+arg1+",arg2:"+arg2);
            DataOnly(arg1, arg2);
        }
        @EventLog
        public void DataOnly(String arg1, String arg2) {}
    }
    public static class IndexedOnlyContract {
        @External
        public void emit(String arg1, String arg2) {
            Context.println("arg1:"+arg1+",arg2:"+arg2);
            IndexedOnly(arg1, arg2);
        }

        @EventLog(indexed = 2)
        public void IndexedOnly(String arg1, String arg2) {}
    }

    public static class IndexedAndDataContract {
        @External
        public void emit(String arg1, String arg2) {
            Context.println("arg1:"+arg1+",arg2:"+arg2);
            IndexedAndData(arg1, arg2);
        }

        @EventLog(indexed = 1)
        public void IndexedAndData(String arg1, String arg2) {}
    }

    private static Account owner = sm.createAccount();
    private static Score dataOnlyScore;
    private static Score indexedOnlyScore;
    private static Score indexedAndDataScore;

    @EventImplement(IndexedOnlyContract.class)
    @EventImplement(value = IndexedAndDataContract.class, suffix = "EventImpl")
    @BeforeAll
    static void setup() throws Exception {
        dataOnlyScore = sm.deploy(owner, EventImplementTestDataOnlyContractEI.class);
        indexedOnlyScore = sm.deploy(owner, EventImplementTestIndexedOnlyContractEI.class);
        indexedAndDataScore = sm.deploy(owner, EventImplementTestIndexedAndDataContractEventImpl.class);
    }

    @Test
    public void emitDataOnly() {
        String arg1 = "a";
        String arg2 = "b";
        dataOnlyScore.invoke(owner, "emit", arg1, arg2);
        var logs = sm.getLastEventLogs();
        assertTrue(logs.contains(new Event(
                dataOnlyScore.getAddress(),
                new Object[]{"DataOnly(str,str)"},
                new Object[]{arg1,arg2})));
    }

    @Test
    public void emitIndexedOnly() {
        String arg1 = "a";
        String arg2 = "b";
        indexedOnlyScore.invoke(owner, "emit", arg1, arg2);
        var logs = sm.getLastEventLogs();
        assertTrue(logs.contains(new Event(
                indexedOnlyScore.getAddress(),
                new Object[]{"IndexedOnly(str,str)",arg1,arg2},
                new Object[]{})));
    }

    @Test
    public void emitIndexedAndData() {
        String arg1 = "a";
        String arg2 = "b";
        indexedAndDataScore.invoke(owner, "emit", arg1, arg2);
        var logs = sm.getLastEventLogs();
        assertTrue(logs.contains(new Event(
                indexedAndDataScore.getAddress(),
                new Object[]{"IndexedAndData(str,str)",arg1},
                new Object[]{arg2})));
    }

}
