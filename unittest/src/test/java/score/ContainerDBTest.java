/*
 * Copyright 2023 ICONLOOP Inc.
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
import com.iconloop.score.test.GenerateTScore;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.annotation.External;
import score.impl.AnyDBImpl;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerDBTest {
    private static ServiceManager sm = ServiceManager.getInstance();

    @GenerateTScore
    public static class TestContract {
        @External(readonly = true)
        public String getStringVarDB(String id) {
            VarDB<String> store = Context.newVarDB(id, String.class);
            return store.get();
        }

        @External(readonly = true)
        public String setStringVarDBInReadOnly(String id, String value) {
            VarDB<String> store = Context.newVarDB(id, String.class);
            store.set(value);
            return store.get();
        }

        @External
        public void setStringVarDB(String id, String value) {
            VarDB<String> store = Context.newVarDB(id, String.class);
            store.set(value);
        }

        @External
        public void setStringVarDBAndRevert(String id, String value) {
            VarDB<String> store = Context.newVarDB(id, String.class);
            store.set(value);
            Context.revert();
        }
    }

    private static Account owner = sm.createAccount();
    private static Score score;

    @BeforeAll
    public static void setup() throws Exception {
        score = sm.deploy(owner, ContainerDBTestTestContractTS.class);
    }

    @Test
    public void preSetTest() {
        final String id = "preset_key";
        final String value = "preset_value1";
        var store = AnyDBImpl.newVarDB(score, id, String.class);
        store.set(value);

        var ret = (String) score.call("getStringVarDB", id);
        assertEquals(value, ret);
    }

    @Test
    public void postCheckTest() {
        final String id = "post_key";
        final String value = "post_value1";

        score.invoke(owner, "setStringVarDB", id, value);

        var store = AnyDBImpl.newVarDB(score, id, String.class);
        assertEquals(value, store.get());
    }

    @Test
    public void emptyAsNull() {
        final String id = "empty";
        score.invoke(owner, "setStringVarDB", id, "");
        var value = (String) score.call("getStringVarDB", id);
        assertNull(value);
    }

    @Test
    public void readonlyTest() {
        final String id = "readonly_key";
        final String value = "readonly_valueX";

        assertThrows(Throwable.class, () -> {
            score.call("setStringVarDBInReadOnly", id, value);
        });

        var store = AnyDBImpl.newVarDB(score, id, String.class);
        assertNull(store.get());
    }

    @Test
    public void revertTest() {
        final String id = "reverting_key";
        final String value1 = "reverting_value1";
        final String value2 = "reverting_value2";

        score.invoke(owner, "setStringVarDB", id, value1);

        assertThrows(Throwable.class, () -> {
                score.invoke(owner, "setStringVarDBAndRevert", id, value2);
        });

        var ret = (String) score.call("getStringVarDB", id);
        assertEquals(value1, ret);
    }
}
