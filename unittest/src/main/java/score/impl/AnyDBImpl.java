/*
 * Copyright 2020 ICONLOOP Inc.
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

import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.lang.reflect.InvocationTargetException;

public class AnyDBImpl extends TestBase implements AnyDB {
    private static final ServiceManager sm = getServiceManager();
    private final String prefix;
    private final Class<?> leafValue;

    private enum Type {
        ArrayDB,
        DictDB,
        VarDB;
    }

    public AnyDBImpl(String id, Class<?> valueClass) {
        this.prefix = id;
        this.leafValue = valueClass;
    }

    private String getSubId(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key was supplied");
        }
        var kv = TypeConverter.toBytes(key);
        var sb = new StringBuilder();
        sb.append(this.prefix);
        sb.append("|");
        sb.append(org.bouncycastle.util.encoders.Hex.toHexString(kv));
        return sb.toString();
    }


    private String getStorageKey(Object k, Type type) {
        return type.name() + getSubId(k);
    }

    private String getStorageKey(Type type) {
        return type.name() + this.prefix;
    }

    private void setValue(String key, Object value) {
        sm.putStorage(key, value);
    }

    private <T> T getValue(Class<T> cls, String key) {
        return sm.getStorage(cls, key);
    }

    // DictDB
    @Override
    public void set(Object key, Object value) {
        if (sm.getCurrentFrame().isReadonly()) {
            throw new IllegalStateException("read-only context");
        }
        setValue(getStorageKey(key, Type.DictDB), value);
    }

    @Override
    public Object get(Object key) {
        return getValue(leafValue, getStorageKey(key, Type.DictDB));
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        var v = getValue(leafValue, getStorageKey(key, Type.DictDB));
        return (v != null) ? v : defaultValue;
    }

    // BranchDB
    @Override
    public Object at(Object key) {
        return new AnyDBImpl(getSubId(key), leafValue);
    }

    // ArrayDB
    @Override
    public void add(Object value) {
        if (sm.getCurrentFrame().isReadonly()) {
            throw new IllegalStateException("read-only context");
        }
        int size = size();
        setValue(getStorageKey(size, Type.ArrayDB), value);
        setValue(getStorageKey(Type.ArrayDB), size + 1);
    }

    @Override
    public void set(int index, Object value) {
        if (sm.getCurrentFrame().isReadonly()) {
            throw new IllegalStateException("read-only context");
        }
        int size = size();
        if (index >= size || index < 0) {
            throw new IllegalArgumentException();
        }
        setValue(getStorageKey(index, Type.ArrayDB), value);
    }

    @Override
    public void removeLast() {
        pop();
    }

    @Override
    public Object get(int index) {
        int size = size();
        if (index >= size || index < 0) {
            throw new IllegalArgumentException();
        }
        return getValue(leafValue, getStorageKey(index, Type.ArrayDB));
    }

    @Override
    public int size() {
        var v = getValue(Integer.class, getStorageKey(Type.ArrayDB));
        if (v == null) return 0;
        return v;
    }

    @Override
    public Object pop() {
        if (sm.getCurrentFrame().isReadonly()) {
            throw new IllegalStateException("read-only context");
        }
        int size = size();
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        var v = getValue(leafValue, getStorageKey(size - 1, Type.ArrayDB));
        setValue(getStorageKey(size - 1, Type.ArrayDB), null);
        setValue(getStorageKey(Type.ArrayDB), size - 1);
        return v;
    }

    // VarDB
    @Override
    public void set(Object value) {
        if (sm.getCurrentFrame().isReadonly()) {
            throw new IllegalStateException("read-only context");
        }
        setValue(getStorageKey(Type.VarDB), value);
    }

    @Override
    public Object get() {
        return getValue(leafValue, getStorageKey(Type.VarDB));
    }

    @Override
    public Object getOrDefault(Object defaultValue) {
        var v = getValue(leafValue, getStorageKey(Type.VarDB));
        return (v != null) ? v : defaultValue;
    }
}
