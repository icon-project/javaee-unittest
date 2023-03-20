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

import foundation.icon.ee.io.DataWriter;
import score.Address;
import score.ByteArrayObjectWriter;
import score.ObjectWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Objects;

public class ObjectWriterImpl implements ByteArrayObjectWriter {
    final private DataWriter writer;
    private int level = 0;

    public ObjectWriterImpl(DataWriter writer) {
        this.writer = writer;
    }

    @Override
    public void write(boolean v) {
        writer.write(v);
    }

    @Override
    public void write(byte v) {
        writer.write(v);
    }

    @Override
    public void write(short v) {
        writer.write(v);
    }

    @Override
    public void write(char v) {
        writer.write(v);
    }

    @Override
    public void write(int v) {
        writer.write(v);
    }

    @Override
    public void write(float v) {
        writer.write(v);
    }

    @Override
    public void write(long v) {
        writer.write(v);
    }

    @Override
    public void write(double v) {
        writer.write(v);
    }

    @Override
    public void write(BigInteger v) {
        writer.write(v);
    }

    @Override
    public void write(String v) {
        writer.write(v);
    }

    @Override
    public void write(byte[] v) {
        writer.write(v);
    }

    @Override
    public void write(Address v) {
        writer.write(v.toByteArray());
    }

    @Override
    public void write(Object v) {
        Objects.requireNonNull(v);

        var c = v.getClass();
        if (c == java.lang.Boolean.class) {
            write((boolean)v);
        } else if (c == java.lang.Byte.class) {
            write((byte)v);
        } else if (c == java.lang.Short.class) {
            write((short)v);
        } else if (c == java.lang.Character.class) {
            write((char)v);
        } else if (c == java.lang.Integer.class) {
            write((int)v);
        } else if (c == java.lang.Float.class) {
            write((float)v);
        } else if (c == java.lang.Long.class) {
            write((long)v);
        } else if (c == java.lang.Double.class) {
            write((double) v);
        } else if (c == java.math.BigInteger.class) {
            write((BigInteger) v);
        } else if (c == java.lang.String.class) {
            write((String) v);
        } else if (c == byte[].class) {
            write((byte[]) v);
        } else if (c == Address.class) {
            write((Address) v);
        } else {
            try {
                var m = c.getDeclaredMethod("writeObject", ObjectWriter.class, c);
                if ((m.getModifiers()& Modifier.STATIC) == 0
                        || (m.getModifiers()&Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException();
                }
                m.invoke(null, this, v);
            } catch (NoSuchMethodException
                     | IllegalAccessException
                     | InvocationTargetException e) {
                e.printStackTrace();
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void writeNullable(Object v) {
        writer.writeNullity(v==null);
        if (v!=null) {
            write(v);
        }
    }

    @Override
    public void write(Object... v) {
        for (var obj : v) {
            write(obj);
        }
    }

    @Override
    public void writeNullable(Object... v) {
        for (Object e : v) {
            writeNullable(e);
        }
    }

    @Override
    public void writeNull() {
        writer.writeNullity(true);
    }

    @Override
    public void beginList(int l) {
        ++level;
        writer.writeListHeader(l);
    }

    @Override
    public void beginNullableList(int l) {
        writer.writeNullity(false);
        beginList(l);
    }

    @Override
    public void writeListOf(Object... v) {
        beginList(v.length);
        write(v);
        end();
    }

    @Override
    public void writeListOfNullable(Object... v) {
        beginList(v.length);
        writeNullable(v);
        end();
    }

    @Override
    public void beginMap(int l) {
        ++level;
        writer.writeMapHeader(l);
    }

    @Override
    public void beginNullableMap(int l) {
        writer.writeNullity(false);
        beginMap(l);
    }

    @Override
    public void end() {
        if (level == 0) {
            throw new IllegalStateException();
        }
        writer.writeFooter();
        --level;
    }

    @Override
    public byte[] toByteArray() {
        return writer.toByteArray();
    }
}
