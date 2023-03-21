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

package foundation.icon.ee.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import score.Address;
import score.Context;

public class ObjectReaderWriterTest {
    private static final String hexDigits = "0123456789abcdef";
    public static String hexFromBytes(byte[] ba) {
        var sb = new StringBuilder();
        for (byte b : ba) {
            sb.append(hexDigits.charAt((b >> 4) & 0xf));
            sb.append(hexDigits.charAt(b & 0xf));
        }
        return sb.toString();
    }

    static void testCodingEquals(String exp, Address v, String codec) {
        var ow = Context.newByteArrayObjectWriter(codec);
        ow.write(v);
        var ba = ow.toByteArray();
        Assertions.assertEquals(exp, hexFromBytes(ba));
        var or = Context.newByteArrayObjectReader(codec, ba);
        Assertions.assertEquals(v, or.readAddress());
    }

    @Test
    void testRLPSimple() {
        testCodingEquals("95" + "00".repeat(21), new Address(new byte[21]), "RLP");
    }

    @Test
    void testRLPNSimple() {
        testCodingEquals("95" + "00".repeat(21), new Address(new byte[21]), "RLPn");
    }
}
