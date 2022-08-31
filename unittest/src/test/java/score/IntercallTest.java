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
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.Test;
import score.annotation.External;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class IntercallTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();

    public static class Callee {
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
    }

    @Test
    void testProxyCall() throws Exception {
        var callee = sm.deploy(owner, Callee.class);
        var caller = sm.deploy(owner, Caller.class);
        assertDoesNotThrow(() ->
                caller.invoke(owner, "proxyCall", callee.getAddress())
        );
    }
}
