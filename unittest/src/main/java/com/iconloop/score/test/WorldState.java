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

import score.Address;
import score.RevertedException;

import java.math.BigInteger;

public class WorldState {
    private DataStore<String,byte[]> store;
    private DataStore<Address,BigInteger> balances;
    private DataStore<Address,Score> scores;

    public WorldState() {
        store = new DataStore<>(null);
        balances = new DataStore<>(null);
        scores = new DataStore<>(null);
    }

    public void setValue(Address addr, String key, byte[] value) {
        if (value == null || value.length == 0) {
            store.set(addr.toString()+key, null);
        } else {
            store.set(addr.toString()+key, value);
        }
    }

    public byte[] getValue(Address addr, String key) {
        return store.get(addr.toString()+key);
    }

    public BigInteger getBalance(Address key) {
        return balances.getOrDefault(key, BigInteger.ZERO);
    }

    public void addBalance(Address key, BigInteger value) {
        balances.set(key,getBalance(key).add(value));
    }

    public void subtractBalance(Address key, BigInteger value) {
        var balance = getBalance(key);
        if (balance.compareTo(value)<0) {
            throw new OutOfBalanceException("OutOfBalance(from="+key+",balance="+balance+",value="+value+")");
        }
        balances.set(key,getBalance(key).subtract(value));
    }

    public Score getScore(Address key) {
        return scores.get(key);
    }

    public void setScore(Address key, Score score) {
        scores.set(key, score);
    }

    public void push() {
        store = new DataStore<>(store);
        balances = new DataStore<>(balances);
        scores = new DataStore<>(scores);
    }

    public void apply() {
        store.apply();
        balances.apply();
        scores.apply();
    }

    public void pop() {
        store = store.parent();
        balances = balances.parent();
        scores = scores.parent();
    }
}
