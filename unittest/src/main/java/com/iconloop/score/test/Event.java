/*
 * Copyright 2023 PARAMETA Inc.
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
import score.impl.TypeConverter;

public class Event {
    private Address contract;
    private Object[] indexed;
    private Object[] data;

    public static final Object[] EMPTY = new Object[0];

    public Event(Address contract, Object[] indexed, Object[] data) {
        if (contract != null && !contract.isContract()) {
            throw new IllegalArgumentException("invalid contract address");
        }
        if (indexed == null || indexed.length<1) {
            throw new IllegalArgumentException("invalid indexed parameter");
        }
        this.contract = contract;
        this.indexed = indexed;
        this.data = data == null ? EMPTY : data;
        for (var obj : this.indexed) {
            if (!TypeConverter.isValidEventValue(obj)) {
                throw new IllegalArgumentException("invalid value type " + obj.getClass());
            }
        }
        for (var obj : this.data) {
            if (!TypeConverter.isValidEventValue(obj)) {
                throw new IllegalArgumentException("invalid value type " + obj.getClass());
            }
        }
    }

    public int numberOfIndexed() {
        return indexed.length;
    }

    public int numberOfData() {
        return data.length;
    }

    public Address getContract() {
        return this.contract;
    }

    public Object getIndexed(int idx) {
        return this.indexed[idx];
    }
    public Object getData(int idx) {
        return this.data[idx];
    }

    private boolean match(Event log, boolean ignoreNull) {
        if (this == log) {
            return true;
        }

        if (indexed.length != log.indexed.length
                || data.length != log.data.length
        ) {
            return false;
        }
        if (contract != null) {
            if (!contract.equals(log.contract)) {
                return false;
            }
        } else {
            if (!ignoreNull && log.contract != null) {
                return false;
            }
        }
        for (int i=0 ; i<indexed.length ; i++) {
            var v1 = indexed[i];
            if (v1==null && ignoreNull) continue;
            var v2 = log.indexed[i];
            var mo = TypeConverter.toBytes(v1);
            var mv = TypeConverter.toBytes(v2);
            if (!java.util.Arrays.equals(mo, mv)) {
                return false;
            }
        }
        for (int i=0 ; i<data.length ; i++) {
            var v1 = data[i];
            if (v1==null && ignoreNull) continue;
            var v2 = log.data[i];
            var mo = TypeConverter.toBytes(v1);
            var mv = TypeConverter.toBytes(v2);
            if (!java.util.Arrays.equals(mo, mv)) {
                return false;
            }
        }
        return true;
    }

    public boolean match(Event log) {
        return this.match(log, true);
    }

    @Override
    public boolean equals(Object log) {
        if (log == null || this.getClass() != log.getClass()) {
            return false;
        }
        return this.match((Event)log, false);
    }

    @Override
    public String toString() {
        return "Event(caller="+contract
                +",indexed="+java.util.Arrays.toString(indexed)
                +",data="+java.util.Arrays.toString(data)+")";
    }
}
