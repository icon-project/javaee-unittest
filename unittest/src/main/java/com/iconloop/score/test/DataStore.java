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

package com.iconloop.score.test;

import java.util.HashMap;
import java.util.Map;

public class DataStore<K,V> {
    private DataStore<K,V> parent;
    private Map<K,V> store;

    public DataStore(DataStore<K,V> parent) {
        this.parent = parent;
        this.store = new HashMap<K,V>();
    }

    public V getOrDefault(K key, V value) {
        var v = get(key);
        if (v == null) {
            return value;
        }
        return v;
    }

    public void set(K key, V value) {
        this.store.put(key, value);
    }

    public V get(K key) {
        var ptr = this;
        while (ptr != null) {
            if (ptr.store.containsKey(key)) {
                return ptr.store.get(key);
            }
            ptr = ptr.parent;
        }
        return null;
    }

    public boolean apply() {
        if (parent!=null) {
            for (var pair : store.entrySet()) {
                parent.store.put(pair.getKey(), pair.getValue());
            }
            return true;
        }
        return false;
    }
    public DataStore<K,V> parent() {
        return parent;
    }
}
