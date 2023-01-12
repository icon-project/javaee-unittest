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
import score.RevertedException;
import score.impl.AnyDBImpl;
import score.impl.TypeConverter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.util.Random;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public abstract class ServiceManager {
    public abstract Score deploy(Account caller, Class<?> mainClass, Object... params) throws Exception;

    public abstract Account createAccount();

    public abstract Account createAccount(int initialIcx);

    public abstract Account getAccount(Address addr);

    /**
     * Invoke specified method for write.
     * It simulates icx_sendTransaction with dataType "call".
     * @param from Sender
     * @param value Value to transfer on call
     * @param targetAddress Receiver of the call
     * @param method Name of the method
     * @param params Parameters for the method
     */
    public abstract void invoke(Account from, BigInteger value, Address targetAddress, String method, Object... params);

    @Deprecated
    public void call(Account from, BigInteger value, Address targetAddress, String method, Object... params) {
        this.invoke(from, value, targetAddress, method, params);
    }

    /**
     * Call specified method for read.
     * It simulates icx_call.
     * @param targetAddress Receiver of the call
     * @param method Name of the method
     * @param params Parameters for the method
     * @return Return value of the method.
     *      It's normalized form.
     *      One of following types are used.
     *      <ul>
     *      <li>BigInteger(for int)</li>
     *      <li>Boolean(for boolean)</li>
     *      <li>byte[](for bytes)</li>
     *      <li>String(for str)</li>
     *      <li>Address</li>
     *      <li>Map(for dict)</li>
     *      <li>Object[](for list)</li>
     *      </ul>
     */
    public abstract Object call(Address targetAddress, String method, Object... params);

    /**
     * Transfer native coin
     * It simulates icx_sendTransaction with no dataType.
     * @param from Sender
     * @param targetAddress Receiver of the call
     * @param value Amount to transfer
     */
    public abstract void transfer(Account from, Address targetAddress, BigInteger value);

    /**
     * Get value of the storage of the contract.
     * @param cls Output object class
     * @param address Contract address
     * @param key Key for the storage
     * @return Deserialized value
     * @param <T> Output return type
     */
    public abstract <T> T getValue(Class<T> cls, Address address, String key);

    /**
     * Set value of the storage of the contract.
     * @param address Contract address
     * @param key Key for the storage
     * @param value Value to be stored
     */
    public abstract void setValue(Address address, String key, Object value);

    public abstract Block getBlock();


    public interface Block {

        long getHeight();

        long getTimestamp();

        void increase();

        void increase(long count);
    }

    private static ServiceManager sInstance;
    private static final String kServiceManagerImplementationClass = "score.ServiceManagerImpl";
    private static final String kMethodNameToGetServiceManager = "getServiceManager";

    public static ServiceManager getInstance() {
        if (sInstance == null) {
            try {
                var sm = Class.forName(kServiceManagerImplementationClass);
                var method = sm.getDeclaredMethod(kMethodNameToGetServiceManager);
                method.setAccessible(true);
                sInstance = (ServiceManager) method.invoke(sm);
            } catch (Exception e) {
                throw new IllegalStateException("UnableToGetServiceManager", e);
            }
        }
        return sInstance;
    }
}
