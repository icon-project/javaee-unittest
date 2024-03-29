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
import scorex.util.ArrayList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score echoScore;

    public static class Echo {
        @External(readonly=true)
        public String echo(String message) {
            return message;
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
}
