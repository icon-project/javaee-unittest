package score.impl;

import org.junit.jupiter.api.Test;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class TypeConverterTest {
    @Test
    void bytes() {
        var cases = new byte[][]{
                new byte[]{0x01, 0x02},
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