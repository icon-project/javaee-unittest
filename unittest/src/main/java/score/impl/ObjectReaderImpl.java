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

import foundation.icon.ee.io.DataReader;
import score.Address;
import score.ObjectReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

public class ObjectReaderImpl implements ObjectReader {
    final private DataReader reader;
    private int level = 0;

    public ObjectReaderImpl(DataReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean readBoolean() {
        return reader.readBoolean();
    }

    @Override
    public byte readByte() {
        return reader.readByte();
    }

    @Override
    public short readShort() {
        return reader.readShort();
    }

    @Override
    public char readChar() {
        return reader.readChar();
    }

    @Override
    public int readInt() {
        return reader.readInt();
    }

    @Override
    public float readFloat() {
        return reader.readFloat();
    }

    @Override
    public long readLong() {
        return reader.readLong();
    }

    @Override
    public double readDouble() {
        return reader.readDouble();
    }

    @Override
    public BigInteger readBigInteger() {
        return reader.readBigInteger();
    }

    @Override
    public String readString() {
        return reader.readString();
    }

    @Override
    public byte[] readByteArray() {
        return reader.readByteArray();
    }

    @Override
    public Address readAddress() {
        return new Address(reader.readByteArray());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> c) {
        if (c == java.lang.Boolean.class) {
            return (T) Boolean.valueOf(readBoolean());
        } else if (c == java.lang.Byte.class) {
            return (T) Byte.valueOf(readByte());
        } else if (c == java.lang.Short.class) {
            return (T) Short.valueOf(readShort());
        } else if (c == java.lang.Character.class) {
            return (T) Character.valueOf(readChar());
        } else if (c == java.lang.Integer.class) {
            return (T) Integer.valueOf(readInt());
        } else if (c == java.lang.Float.class) {
            return (T) Float.valueOf(readFloat());
        } else if (c == java.lang.Long.class) {
            return (T) Long.valueOf(readLong());
        } else if (c == java.lang.Double.class) {
            return (T) Double.valueOf(readDouble());
        } else if (c == java.lang.String.class) {
            return (T) readString();
        } else if (c == java.math.BigInteger.class) {
            return (T) readBigInteger();
        } else if (c == byte[].class) {
            return (T) readByteArray();
        } else if (c == Address.class) {
            return (T) readAddress();
        } else {
            try {
                var m = c.getDeclaredMethod("readObject", ObjectReader.class);
                if ((m.getModifiers()& Modifier.STATIC) == 0
                        || (m.getModifiers()&Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException();
                }
                var res = m.invoke(null, this);
                return (T) res;
            } catch (NoSuchMethodException
                     | IllegalAccessException
                     | InvocationTargetException e) {
                e.printStackTrace();
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public <T> T readOrDefault(Class<T> c, T def) {
        if (!hasNext()) {
            return def;
        }
        return read(c);
    }

    @Override
    public <T> T readNullable(Class<T> c) {
        if (reader.readNullity()) {
            return null;
        }
        return read(c);
    }

    @Override
    public <T> T readNullableOrDefault(Class<T> c, T def) {
        if (reader.readNullity()) {
            return def;
        }
        return readNullable(c);
    }

    @Override
    public void beginList() {
        level++;
        reader.readListHeader();
    }

    @Override
    public boolean beginNullableList() {
        if (reader.readNullity()) {
            return false;
        }
        beginList();
        return true;
    }

    @Override
    public void beginMap() {
        level++;
        reader.readMapHeader();
    }

    @Override
    public boolean beginNullableMap() {
        if (reader.readNullity()) {
            return false;
        }
        beginMap();
        return true;
    }

    @Override
    public void skip(int count) {
        reader.skip(count);
    }

    @Override
    public boolean hasNext() {
        return reader.hasNext();
    }

    @Override
    public void end() {
        if (level == 0) {
            throw new IllegalStateException();
        }
        while (reader.hasNext()) {
            reader.skip(1);
        }
        reader.readFooter();
        --level;
    }

    @Override
    public void skip() {
        skip(1);
    }
}
