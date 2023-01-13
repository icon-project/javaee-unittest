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

import java.math.BigInteger;

public abstract class ServiceManager {
    public abstract Score deploy(Account caller, Class<?> mainClass, Object... params) throws Exception;

    /**
     * Create new EoA account.
     * @return created account
     */
    public abstract Account createAccount();

    /**
     * Create new EoA account with specified balance.
     * @param initialIcx Initial balance of the account
     * @return created account
     */
    public abstract Account createAccount(int initialIcx);

    /**
     * Get account.
     * @param addr Address of the account
     * @return If it's already created, then it returns
     *          otherwise, it would be created.
     */
    public abstract Account getAccount(Address addr);

    /**
     * Create new dummy smart contract account.
     * @return created smart contract account.
     */
    public abstract Account createScoreAccount();

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
     * @return Return value of the method as it is
     */
    public abstract Object call(Address targetAddress, String method, Object... params);

    /**
     * Call specified method for read.
     * It simulates icx_call.
     * @param cls Return object type
     * @param targetAddress Receiver of the call
     * @param method Name of the method
     * @param params Parameters for the method
     * @return Return value of the method converted to the specified type.
     * @param <T> Return type
     *           It will throw exception on prohibited types.
     */
    public abstract <T> T call(Class<T> cls, Address targetAddress, String method, Object... params);

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

    /**
     * Get last block information.
     * @return last block information
     */
    public abstract Block getBlock();


    public interface Block {
        /**
         * Get height of the block
         * @return Height of the current block.
         */
        long getHeight();

        /**
         * Get timestamp of the block
         * @return Timestamp of the block in micro-second
         */
        long getTimestamp();

        /**
         * Increase last block height by one.
         * <p>
         * To get updated block information, use {@link #getBlock()}.
         * @see #increase(long)
         */
        void increase();

        /**
         * Increase last block height
         * <p>
         * To get updated block information, use {@link #getBlock()}.
         * @param count amount of height to increase
         * @see #increase()
         * @see #getBlock()
         */
        void increase(long count);

        /**
         * Calculate hash of transaction at the index
         * <p>
         * It can be used to match transaction hash retrieved by
         * {@link score.Context#getTransactionHash()}
         *
         * @param idx Index of the transaction
         * @return the hash value used for the transaction
         */
        byte[] hashOfTransactionAt(int idx);
    }

    private static ServiceManager sInstance;
    private static final String kServiceManagerImplementationClass = "score.ServiceManagerImpl";
    private static final String kMethodNameToGetServiceManager = "getServiceManager";

    /**
     * Get singleton instance.
     * @return singleton instance of it
     */
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
