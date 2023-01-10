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

package score.impl;

import score.Address;
import score.impl.struct.Property;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeConverter {
    public static Object normalize(Object so) {
        if (so==null) {
            return null;
        }
        var clz = so.getClass();
        if (so instanceof Boolean) {
            return so;
        } else if (so instanceof Character) {
            return BigInteger.valueOf((char)so);
        } else if (so instanceof Byte) {
            return BigInteger.valueOf((byte)so);
        } else if (so instanceof Short) {
            return BigInteger.valueOf((short)so);
        } else if (so instanceof Integer) {
            return BigInteger.valueOf((int)so);
        } else if (so instanceof Long) {
            return BigInteger.valueOf((long)so);
        } else if (so instanceof java.lang.String) {
            return so;
        } else if (so instanceof java.math.BigInteger) {
            return so;
        } else if (so instanceof score.Address) {
            var o = (score.Address)so;
            return new Address(o.toByteArray());
        } else if (so instanceof byte[]) {
            var o = (byte[]) so;
            var no = new byte[o.length];
            System.arraycopy(o, 0, no, 0, o.length);
            return no;
        } else if (so instanceof boolean[]) {
            var o = (char[]) so;
            var no = new Object[o.length];
            for (int i = 0 ; i<o.length ; i++) {
                no[i] = BigInteger.valueOf(o[i]);
            }
            return no;
        } else if (so instanceof char[]) {
            var o = (char[]) so;
            var no = new Object[o.length];
            for (int i = 0 ; i<o.length ; i++) {
                no[i] = BigInteger.valueOf(o[i]);
            }
            return no;
        } else if (so instanceof short[]) {
            var o = (short[]) so;
            var no = new Object[o.length];
            for (int i = 0 ; i<o.length ; i++) {
                no[i] = BigInteger.valueOf(o[i]);
            }
            return no;
        } else if (so instanceof int[]) {
            var o = (int[]) so;
            var no = new Object[o.length];
            for (int i = 0 ; i<o.length ; i++) {
                no[i] = BigInteger.valueOf(o[i]);
            }
            return no;
        } else if (so instanceof long[]) {
            var o = (long[]) so;
            var no = new Object[o.length];
            for (int i = 0; i < o.length; i++) {
                no[i] = BigInteger.valueOf(o[i]);
            }
            return no;
        } else if (so instanceof List) {
            var o = (List)so;
            var no = new Object[o.size()];
            for (int i=0 ; i<no.length ; i++) {
                no[i] = normalize(o.get(i));
            }
            return no;
        } else if (so instanceof Map) {
            var o = (Map<String,Object>)so;
            var no = new LinkedHashMap<>();
            for (Map.Entry<String,Object> pair : o.entrySet()) {
                no.put(pair.getKey(), normalize(pair.getValue()));
            }
            return no;
        } else if (clz.isArray()){
            var o = (Object[])so;
            var no = new Object[o.length];
            for (int i=0 ; i<o.length ; i++) {
                no[i] = normalize(o[i]);
            }
            return no;
        } else {
            var rProps = Property.getReadableProperties(so);
            if (rProps.isEmpty()) {
                throw new IllegalArgumentException();
            }
            var map = new java.util.TreeMap<>();
            for (var rp : rProps) {
                try {
                    map.put(rp.getName(), normalize(rp.get(so)));
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return map;
        }
    }

    private static final BigInteger CharacterMAX = BigInteger.valueOf(Character.MAX_VALUE);
    private static final BigInteger CharacterMIN = BigInteger.valueOf(Character.MIN_VALUE);
    protected static void requireCharacterRange(BigInteger value) {
        if (value.compareTo(CharacterMAX) > 0 || value.compareTo(CharacterMIN)<0) {
            throw new ArithmeticException("out of char range");
        }
    }

    public static Object castArray(Object so, Class<?> arrayCls) {
        var cls = arrayCls.getComponentType();
        if (cls == char.class) {
            var o = (Object[])so;
            var no = new char[o.length];
            for (int i=0 ; i<o.length ; i++) {
                var value = (BigInteger)o[i];
                requireCharacterRange(value);
                no[i] = (char)value.intValueExact();
            }
            return no;
        } else if (cls == boolean.class) {
            var o = (Object[])so;
            var no = new boolean[o.length];
            for (int i=0 ; i<o.length ; i++) {
                no[i] = (Boolean)o[i];
            }
            return no;
        } else if (cls == short.class) {
            var o = (Object[])so;
            var no = new short[o.length];
            for (int i=0 ; i<o.length ; i++) {
                no[i] = ((BigInteger)o[i]).shortValueExact();
            }
            return no;
        } else if (cls == int.class) {
            var o = (Object[])so;
            var no = new int[o.length];
            for (int i=0 ; i<o.length ; i++) {
                no[i] = ((BigInteger)o[i]).intValueExact();
            }
            return no;
        } else if (cls == long.class) {
            var o = (Object[])so;
            var no = new long[o.length];
            for (int i=0 ; i<o.length ; i++) {
                no[i] = ((BigInteger)o[i]).longValueExact();
            }
            return no;
        } else {
            var o = (Object[])so;
            var no = Array.newInstance(cls, o.length);
            for (int i=0 ; i<o.length ; i++) {
                Array.set(no, i, specialize(o[i], cls));
            }
            return no;
        }
    }

    public static<T> T cast(Object so, Class<T> cls) {
        return (T)specialize(normalize(so), cls);
    }

    public static Object specialize(Object so, Class<?> cls) {
        if (so == null ) {
            return null;
        }
        if (cls == so.getClass() || cls == boolean.class) {
            return so;
        } else if (cls == byte.class) {
            var o = (BigInteger)so;
            return o.byteValueExact();
        } else if (cls == char.class) {
            var o = (BigInteger)so;
            requireCharacterRange(o);
            return (char)o.intValue();
        } else if (cls == short.class) {
            var o = (BigInteger)so;
            return o.shortValueExact();
        } else if (cls == int.class) {
            return ((BigInteger)so).intValueExact();
        } else if (cls == long.class) {
            return ((BigInteger) so).longValueExact();
        } else if (cls == List.class) {
            var o = (Object[])so;
            return List.of(o);
        } else if (cls == Map.class) {
            var o = (Map<String, Object>)so;
            return Map.copyOf(o);
        } else if (cls.isArray()) {
            return castArray(so, cls);
        } else if (cls == Address.class) {
            return (Address)so;
        } else if (so instanceof java.util.Map) {
            // struct handling
            try {
                @SuppressWarnings("unchecked")
                var o = (Map<String, Object>)so;
                var ctor = cls.getConstructor();
                var res = ctor.newInstance();
                for (var e : o.entrySet()) {
                    var wp = Property.getWritableProperty(cls, e.getKey());
                    if (wp == null) {
                        throw new IllegalArgumentException("no prop for "+e.getKey());
                    }
                    wp.set(res, specialize(e.getValue(), wp.getType()));
                }
                return res;
            } catch (NoSuchMethodException
                     | IllegalAccessException
                     | InstantiationException
                     | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("invalid parameter type source="+so.getClass().getName()
                    +" target="+cls.getName());
        }
    }

    public static byte[] toBytes(Object v) {
        if (v == null) {
            throw new IllegalArgumentException("null for key");
        } else if (v instanceof String) {
            return ((String)v).getBytes(StandardCharsets.UTF_8);
        } else if (v instanceof byte[]) {
            var value = (byte[])v;
            return Arrays.copyOf(value, value.length);
        } else if (v instanceof Boolean) {
            return BigInteger.valueOf(((Boolean)v) ? 1 : 0).toByteArray();
        } else if (v instanceof Byte) {
            return BigInteger.valueOf((Byte)v).toByteArray();
        } else if (v instanceof Character) {
            return BigInteger.valueOf((Character)v).toByteArray();
        } else if (v instanceof Short) {
            return BigInteger.valueOf((Short)v).toByteArray();
        } else if (v instanceof Integer) {
            return BigInteger.valueOf((Integer) v).toByteArray();
        } else if (v instanceof Long) {
            return BigInteger.valueOf((Long) v).toByteArray();
        } else if (v instanceof BigInteger) {
            return ((BigInteger) v).toByteArray();
        } else if (v instanceof Address) {
            return ((Address) v).toByteArray();
        } else {
            var w = new RLPObjectWriter();
            w.write(v);
            return w.toByteArray();
        }
    }

    public static <T> T fromBytes(Class<T> cls, byte[] bs) {
        return cls.cast(fromBytesReal(cls, bs));
    }

    private static Object fromBytesReal(Class<?> cls, byte[] bs) {
        if (bs==null) {
            return null;
        }
        if (cls == byte[].class) {
            return Arrays.copyOf(bs, bs.length);
        } else if (cls == Boolean.class) {
            if (bs.length==0) {
                return Boolean.FALSE;
            }
            if (bs.length != 1) {
                throw new IllegalArgumentException("invalid length for boolean len=" + bs.length);
            }
            if (bs[0] == 1) {
                return Boolean.TRUE;
            } else if (bs[0] == 0){
                return Boolean.FALSE;
            } else {
                throw new IllegalArgumentException(("invalid value for boolean value="+bs[0]));
            }
        } else if (cls == Byte.class) {
            var value = new BigInteger(bs);
            return Byte.valueOf(value.byteValueExact());
        } else if (cls == Character.class) {
            var value = new BigInteger(bs);
            requireCharacterRange(value);
            return Character.valueOf((char)value.intValue());
        } else if (cls == Short.class) {
            var value = new BigInteger(bs);
            return Short.valueOf(value.shortValueExact());
        } else if (cls == Integer.class) {
            var value = new BigInteger(bs);
            return Integer.valueOf(value.intValueExact());
        } else if (cls == Long.class) {
            var value = new BigInteger(bs);
            return Long.valueOf(value.longValueExact());
        } else if (cls == BigInteger.class) {
            return new BigInteger(bs);
        } else if (cls == String.class) {
            return new String(bs, StandardCharsets.UTF_8);
        } else if (cls == Address.class) {
            return new Address(bs);
        } else {
            var r = new RLPObjectReader(bs);
            return r.read(cls);
        }
    }
}
