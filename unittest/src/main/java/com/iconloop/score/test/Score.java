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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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

    private Object getReturnValue (Object returnValue) throws IllegalAccessException {
        // Nothing returned
        if (returnValue == null) {
            return returnValue;
        }

        // The result type needs to be post-processed as SCORE intercalls may change the type
        Class<?> returnType = returnValue.getClass();

        if ( // These native types don't need any modification
            (returnType == BigInteger.class)
        ||  (returnType == Address.class)
        ||  (returnType == Boolean.class)
        ||  (returnType == String.class)
        ||  (returnType == byte[].class)
        ) {
            return returnValue;
        }

        // Convert Integer native classes to BigInteger
        if (returnType == Integer.class) {
            return BigInteger.valueOf((Integer) returnValue);
        } else if (returnType == Long.class) {
            return BigInteger.valueOf((Long) returnValue);
        } else if (returnType == Short.class) {
            return BigInteger.valueOf((Short) returnValue);
        } else if (returnType == Character.class) {
            return BigInteger.valueOf((Character) returnValue);
        } else if (returnType == Byte.class) {
            return BigInteger.valueOf((Byte) returnValue);
        }

        // The result is not a native SCORE type
        // Is it an Array ?
        if (returnType.isArray()) {
            int arrayLength = Array.getLength(returnValue);
            if (arrayLength == 0) {
                return new Object[] {};
            }

            var firstItem = getReturnValue(Array.get(returnValue, 0));
            Object[] returnArray = (Object[]) Array.newInstance(firstItem.getClass(), arrayLength);

            for (int i = 0; i < Array.getLength(returnValue); i++) {
                // Decide what to do with each array item, recursive call it
                returnArray[i] = getReturnValue(Array.get(returnValue, i));
            }
            return returnArray;
        }

        // Only remaining possibility : it's a user class that needs to be converted to Map
        Map<String, Object> returnMap = new HashMap<String, Object>();
        for (var field : returnType.getFields()) {
            // Decide what to do with field values, recursive call it
            var fieldValue = getReturnValue(field.get(returnValue));
            returnMap.put(field.getName(), fieldValue);
        }
        return returnMap;
    }

    Object call(Account from, boolean readonly, BigInteger value, String method, Object... params) {
        sm.pushFrame(from, this.score, readonly, method, value);
        Class<?>[] paramClasses = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> type = params[i].getClass();
            // Convert supported object types to primitive data types
            if (type == Integer.class) {
                paramClasses[i] = Integer.TYPE; // int
            } else if (type == Long.class) {
                paramClasses[i] = Long.TYPE; // long
            } else if (type == Short.class) {
                paramClasses[i] = Short.TYPE; // short
            } else if (type == Character.class) {
                paramClasses[i] = Character.TYPE; // char
            } else if (type == Byte.class) {
                paramClasses[i] = Byte.TYPE; // byte
            } else if (type == Boolean.class) {
                paramClasses[i] = Boolean.TYPE; // boolean
            } else {
                paramClasses[i] = type;
            }
        }
        try {
            Class<?> clazz = instance.getClass();
            var m = clazz.getMethod(method, paramClasses);
            var result = m.invoke(instance, params);
            return getReturnValue(result);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new AssertionError(e.getTargetException().getMessage());
        } finally {
            sm.popFrame();
        }
    }
}
