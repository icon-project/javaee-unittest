/*
 * Copyright 2024 PARAMETA Corp.
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
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.Test;
import score.annotation.External;
import score.annotation.Optional;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConstructorTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();

    public static class DataHolder {
        private final int value1;
        private final int value2;
        public DataHolder(int value1, @Optional int value2) {
            this.value1 = value1;
            this.value2 = value2;
        }

        @External(readonly = true)
        public int getValue1() {
            return value1;
        }

        @External(readonly = true)
        public int getValue2() {
            return value2;
        }
    }

    @GenerateTScore(DataHolder.class)
    @Test
    void deployTest() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            var score = sm.deploy(owner, ConstructorTestDataHolderTS.class);
        });

        var score = sm.deploy(owner, ConstructorTestDataHolderTS.class, 1);
        var value1 = score.call(int.class, "getValue1");
        var value2 = score.call(int.class, "getValue2");
        assertEquals(1, value1);
        assertEquals(0, value2);

        score = sm.deploy(owner, ConstructorTestDataHolderTS.class, 2, 3);
        value1 = score.call(int.class, "getValue1");
        value2 = score.call(int.class, "getValue2");
        assertEquals(2, value1);
        assertEquals(3, value2);
    }
}
