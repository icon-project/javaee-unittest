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

package com.iconloop.score.test;

import score.Address;
import score.UserRevertedException;
import score.impl.AnyDBImpl;
import score.impl.TypeConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

public class Score extends TestBase implements AnyDBImpl.ValueStore {
    private static final ServiceManager sm = getServiceManager();

    private final Account score;
    private final Account owner;
    private Object instance;

    public Score(Account score, Account owner) {
        this.score = score;
        this.owner = owner;
    }

    public Account getAccount() {
        return this.score;
    }

    public Address getAddress() {
        return this.score.getAddress();
    }

    public Account getOwner() {
        return this.owner;
    }

    public void setInstance(Object newInstance) {
        this.instance = newInstance;
    }

    public Object getInstance() {
        return this.instance;
    }

    public Object call(String method, Object... params) {
        return sm.call(getAddress(), method, params);
    }

    public void invoke(Account from, String method, Object... params) {
        invoke(from, BigInteger.ZERO, method, params);
    }

    public void invoke(Account from, BigInteger value, String method, Object... params) {
        sm.invoke(from, value, getAddress(), method, params);
    }

    @Override
    public <T> T getValue(Class<T> cls, String key) {
        return sm.getValue(cls, getAddress(), key);
    }

    @Override
    public void setValue(String key, Object value) {
        sm.setValue(getAddress(), key, value);
    }
}
