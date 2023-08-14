package score.impl;

import org.junit.jupiter.api.Test;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class TypeConverterTest {
    @Test
    void bytes() {
        var cases = new byte[][]{
                new byte[]{0x01, 0x02},
                new byte[]{(byte)0x83},
                new byte[]{},
        };
        for (var tc : cases) {
            var bs = TypeConverter.toBytes(tc);
            var real = TypeConverter.fromBytes(byte[].class, bs);
            assertArrayEquals(tc, real);
        }
    }

    @Test
    void primitives() {
        var cases = new Object[] {
                Boolean.FALSE,
                Boolean.TRUE,
                Character.valueOf('c'),
                Character.MAX_VALUE,
                Character.MIN_VALUE,
                'z',
                3,
                -349,
                (long)123,
                (long)-123,
                (byte)7,
                (short)889,
                "Test String",
                Address.fromString("hx0800000000000000000000000000000000000000"),
        };
        for (var tc : cases) {
            var bs = TypeConverter.toBytes(tc);
            var real = TypeConverter.fromBytes(tc.getClass(), bs);
            assertEquals(tc, real);
        }
    }

    @Test
    void normalizeList() {
        var cases = new Object[] {
                List.of("a", "b", "c"),
                List.of(BigInteger.ZERO, BigInteger.ONE),
                List.of(new byte[]{0x1, 0x2}, new byte[]{0x3, 0x4}),
                List.of(Map.of("a", BigInteger.ZERO), Map.of("b", BigInteger.ONE)),
                List.of(Address.fromString("hx0800000000000000000000000000000000000000")),
                new boolean[]{false, true},
                new int[]{1, 2, 3},
                new long[]{1L, 2L, 3L},
                new String[]{"a", "b", "c"},
                new BigInteger[]{BigInteger.ZERO, BigInteger.ONE},
                new Address[]{Address.fromString("hx0800000000000000000000000000000000000000")}
        };
        for (var tc : cases) {
            assertDoesNotThrow(() -> (List<Object>) TypeConverter.normalize(tc));
        }
    }

    @Test
    void normalizeMap() {
        var cases = new Object[] {
                Map.of("a", BigInteger.ZERO, "b", BigInteger.ONE),
                Map.of("a", List.of("a", "b", "c")),
                Map.of("a", List.of(Map.of("a", BigInteger.ZERO)))
        };
        for (var tc : cases) {
            assertDoesNotThrow(() -> (Map<String, Object>) TypeConverter.normalize(tc));
        }
    }

    public static class TestObject {
        public int intValue;
        public String stringValue;

        public TestObject(int value, String str) {
            intValue = value;
            stringValue = str;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != TestObject.class) {
                return false;
            }
            var o = (TestObject)obj;
            return o.intValue == intValue && (
                    Objects.equals(stringValue, o.stringValue)
            );
        }

        public static void writeObject(ObjectWriter w, TestObject obj) {
            w.writeListOfNullable(obj.intValue, obj.stringValue);
        }
        public static TestObject readObject(ObjectReader r) {
            r.beginList();
            var obj = new TestObject(r.readInt(), r.readNullable(String.class));
            r.end();
            return obj;
        }
    }

    @Test
    void objectStore() {
        var cases = new Object[] {
                new TestObject(3, "TEST"),
                new TestObject(7, null),
        };

        for (var tc : cases) {
            var bs = TypeConverter.toBytes(tc);
            var obj = TypeConverter.fromBytes(TestObject.class, bs);
            assertEquals(tc, obj);
        }
    }
}