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
import score.impl.AnyDBImpl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Account {
    private static final ServiceManager sm = ServiceManager.getInstance();

    final private Address address;
    final private WorldState state;
    final private Map<String, BigInteger> tokens = new HashMap<>();
    final private static Map<Address,Account> accounts = new HashMap<>();

    /**
     * Get address of the account
     * @return Address of the account
     */
    public Address getAddress() {
        return address;
    }

    public Account(WorldState state, Address address) {
        this.state = state;
        this.address = address;
        accounts.put(address, this);
    }

    @Override
    public String toString() {
        return address.toString();
    }

    /**
     * Get current balance of the account
     * @return balance of the account
     */
    public BigInteger getBalance() {
        return state.getBalance(address);
    }

    /**
     * Increase balance of the account
     * @param value Amount to increase
     */
    public void addBalance(BigInteger value) {
        state.addBalance(address, value);
    }

    /**
     * Decrease balance of the account
     * @param value Amount to increase
     */
    public void subtractBalance(BigInteger value) {
        state.subtractBalance(address, value);
    }

    /**
     * Get stored balance for specified token
     * @param symbol symbol of the token
     * @return stored balance for the totken
     */
    public BigInteger getBalance(String symbol) {
        return tokens.getOrDefault(symbol, BigInteger.ZERO);
    }

    /**
     * Add specified value to stored balance
     * @param symbol symbol of the token
     * @param value value to be added
     */
    public void addBalance(String symbol, BigInteger value) {
        if (value.signum()<0) {
            throw new IllegalArgumentException("negative value change");
        }
        var balance = getBalance(symbol);
        tokens.put(symbol, balance.add(value));
    }

    /**
     * Subtract specified value from stored balance
     * @param symbol symbol of the token
     * @param value value to be subtracted
     */
    public void subtractBalance(String symbol, BigInteger value) {
        if (value.signum()<0) {
            throw new IllegalArgumentException("negative value change");
        }
        var balance = getBalance(symbol);
        if (balance.compareTo(value)<0) {
            throw new IllegalArgumentException("out of balance");
        }
        tokens.put(symbol, balance.subtract(value));
    }

    /**
     * Get the account with the address
     * @param address Address of the account
     * @return Account for accessing the account data
     */
    public static Account getAccount(Address address) {
        return accounts.get(address);
    }

    public static Account accountOf(WorldState state, Address address) {
        var acct = accounts.get(address);
        if (acct != null) {
            if (acct.state != state) {
                throw new IllegalStateException("different state for same address");
            }
            return acct;
        } else {
            return new Account(state, address);
        }
    }

    /**
     * New dummy smart contract account
     * @param seed seed for creating the account
     * @return created account instance
     * @deprecated Replaced by {@link ServiceManager#createScoreAccount()}
     */
    @Deprecated
    public static Account newScoreAccount(int seed) {
        return sm.createScoreAccount();
    }
}
