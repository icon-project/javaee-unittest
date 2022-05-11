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
        public String castedEcho(String message) {
            return (String) Context.call(Context.getAddress(), "echo", message);
        }

        @External(readonly=true)
        public String typedEcho(String message) {
            return Context.call(String.class, Context.getAddress(), "echo", message);
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
}
