/*
 * Copyright 2021 ICONLOOP Inc.
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
import score.impl.Crypto;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score helloScore;

    @BeforeAll
    static void setUp() throws Exception {
        helloScore = sm.deploy(owner, HelloWorld.class, "Alice");
    }

    @Test
    void getAddress() {
        assertEquals(helloScore.getAddress(), helloScore.call("getAddress"));
    }

    @Test
    void getOwner() {
        assertEquals(owner.getAddress(), helloScore.call("getOwner"));
    }

    @Test
    void getBlockTimestamp() {
        long start = sm.getBlock().getTimestamp();
        assertEquals(start, helloScore.call("getBlockTimestamp"));
        sm.getBlock().increase(100);
        assertEquals(start + 2_000_000 * 100, helloScore.call("getBlockTimestamp"));
    }

    @Test
    void hash() {
        String[] algorithms = new String[]{"sha3-256", "keccak-256"};
        byte[] data = "Hello world".getBytes();
        for (String algo : algorithms) {
            assertArrayEquals(Crypto.hash(algo, data),
                    (byte[]) helloScore.call("computeHash", algo, data));
        }
    }

    @Test
    void getBalance() {
        var exp = BigInteger.TEN.pow(18).multiply(BigInteger.valueOf(100));
        var acct = sm.createAccount(100);
        assertEquals(exp, acct.getBalance());
        var balance = Context.getBalance(acct.getAddress());
        assertEquals(exp, balance);
    }

    @Test
    void accountEqualsTest() {
        assertEquals(owner, sm.getAccount(owner.getAddress()));
    }
}
