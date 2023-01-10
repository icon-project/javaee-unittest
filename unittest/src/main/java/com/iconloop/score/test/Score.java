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
import score.impl.TypeConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

public class Score extends TestBase {
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
        return call(null, true, BigInteger.ZERO, method, params);
    }

    public void invoke(Account from, String method, Object... params) {
        sm.getBlock().increase();
        call(from, false, BigInteger.ZERO, method, params);
    }

    public void invoke(Account from, BigInteger value, String method, Object... params) {
        sm.getBlock().increase();
        call(from, false, value, method, params);
    }

    Object invokeMethod(String methodName, Object... params) {
        try {
            Method method = getMethodByName(methodName);
            Object[] methodParameters = convertParameters(method, params);

            var result = method.invoke(instance, methodParameters);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InvocationTargetException e) {
            var target = e.getCause();
            if (target instanceof UserRevertedException
                    && sm.getCurrentFrame() != sm.getFirstFrame()) {
                throw (UserRevertedException) target;
            }
            throw new AssertionError(target.getMessage());
        }
    }

    Object call(Account from, boolean readonly, BigInteger value, String methodName, Object... params) {
        return sm.call(from, value, true, readonly, this.score.getAddress(), methodName, params);
    }

    private Object[] convertParameters(Method method, Object[] params) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        int numberOfParams = parameterTypes.length;
        Object[] parsedParams = new Object[numberOfParams];

        int i = 0;
        for (Class<?> parameterClass : parameterTypes) {
            if (i>=params.length) {
                parsedParams[i] = null;
            } else {
                try {
                    parsedParams[i] = TypeConverter.cast(params[i], parameterClass);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("invalid parameter", e);
                }
            }
            i++;
        }
        return parsedParams;
    }

    private Method getMethodByName(String name) throws NoSuchMethodException {
        Class<?> clazz = instance.getClass();
        Method[] m = clazz.getMethods();
        for (Method method : m) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new NoSuchMethodException();
    }
}
