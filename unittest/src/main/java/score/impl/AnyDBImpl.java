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

import score.ArrayDB;
import score.BranchDB;
import score.DictDB;
import score.VarDB;


/**
 * Implementation class for {@link AnyDB} interface.
 * @see AnyDB
 */
public class AnyDBImpl implements AnyDB {
    private final ValueStore store;
    private final String prefix;
    private final Class<?> leafValue;

    private enum Type {
        ArrayDB,
        DictDB,
        VarDB;
    }

    public interface ValueStore  {
        <T> T getValue(Class<T> cls, String key);
        void setValue(String key, Object value);
    }

    private AnyDBImpl(ValueStore store, String id, Class<?> valueClass) {
        this.store = store;
        this.prefix = id;
        this.leafValue = valueClass;
    }

    private String getSubId(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key was supplied");
        }
        var kv = TypeConverter.toBytes(key);
        return this.prefix + "|" + org.bouncycastle.util.encoders.Hex.toHexString(kv);
    }


    private String getStorageKey(Object k, Type type) {
        return type.name() + getSubId(k);
    }

    private String getStorageKey(Type type) {
        return type.name() + this.prefix;
    }

    private void setValue(String key, Object value) {
        store.setValue(key, value);
    }

    private <T> T getValue(Class<T> cls, String key) {
        return store.getValue(cls, key);
    }

    // DictDB
    @Override
    public void set(Object key, Object value) {
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
        return new AnyDBImpl(store, getSubId(key), leafValue);
    }

    // ArrayDB
    @Override
    public void add(Object value) {
        int size = size();
        setValue(getStorageKey(size, Type.ArrayDB), value);
        setValue(getStorageKey(Type.ArrayDB), size + 1);
    }

    @Override
    public void set(int index, Object value) {
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

    /**
     * Make new BranchDB to access the store.
     * @param store Data store
     * @param id Identifier to the storage
     * @param leafClass Class to be used for deserializing.
     * @return New {@link score.BranchDB}
     * @param <K> Key type
     * @param <V> Child type (reaching to the {@code leafClass}).
     * @see com.iconloop.score.test.Score
     */
    @SuppressWarnings("unchecked")
    public static<K,V> BranchDB<K,V> newBranchDB(ValueStore store, String id, Class<?> leafClass) {
        return new AnyDBImpl(store, id, leafClass);
    }

    /**
     * Make new DictDB to access the store.
     * @param store Data store
     * @param id Identifier to the storage
     * @param leafClass Class to be used for deserializing.
     * @return New {@link score.DictDB}
     * @param <K> Key type
     * @param <V> Value type
     * @see com.iconloop.score.test.Score
     */
    @SuppressWarnings("unchecked")
    public static<K,V> DictDB<K,V> newDictDB(ValueStore store, String id, Class<V> leafClass) {
        return new AnyDBImpl(store, id, leafClass);
    }

    /**
     * Make new ArrayDB to access the store
     * @param store Data store
     * @param id Identifier to the storage
     * @param leafClass Class to be used for deserializing.
     * @return New {@link score.ArrayDB}
     * @param <E> Value type
     * @see com.iconloop.score.test.Score
     */
    @SuppressWarnings("unchecked")
    public static<E> ArrayDB<E> newArrayDB(ValueStore store, String id, Class<E> leafClass) {
        return new AnyDBImpl(store, id, leafClass);
    }

    /**
     * Make new VarDB to access the store
     * @param store Data store
     * @param id Identifier to the storage
     * @param leafClass Class to be used for deserializing.
     * @return New {@link score.ArrayDB}
     * @param <E> Value type
     * @see com.iconloop.score.test.Score
     */
    @SuppressWarnings("unchecked")
    public static<E> VarDB<E> newVarDB(ValueStore store, String id, Class<E> leafClass) {
        return new AnyDBImpl(store, id, leafClass);
    }
}
