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

//    @EventImplement
    public static class Contract {

        @External
        public void invoke(String arg1, String arg2) {
            DataOnly(arg1, arg2);
            IndexedOnly(arg1, arg2);
            IndexedAndData(arg1, arg2);
        }
        @EventLog
        public void DataOnly(String arg1, String arg2) {}

        @EventLog(indexed = 2)
        public void IndexedOnly(String arg1, String arg2) {}

        @EventLog(indexed = 1)
        public void IndexedAndData(String arg1, String arg2) {}
    }

    private static Account owner = sm.createAccount();
    private static Score score;

    @BeforeAll
    static void setup() throws Exception {
        score = sm.deploy(owner, EventImplementTestContractEI.class);
    }

    @EventImplement("score.EventImplementTest.Contract")
    @Test
    public void invoke() {
        score.invoke(owner, "invoke", "a", "b");
        var logs = sm.getLastEventLogs();
        assertTrue(logs.contains(new Event(
                score.getAddress(),
                new Object[]{"DataOnly(str,str)"},
                new Object[]{"a","b"})));
        assertTrue(logs.contains(new Event(
                score.getAddress(),
                new Object[]{"IndexedOnly(str,str)","a","b"},
                new Object[]{})));
        assertTrue(logs.contains(new Event(
                score.getAddress(),
                new Object[]{"IndexedAndData(str,str)","a"},
                new Object[]{"b"})));
    }
}
