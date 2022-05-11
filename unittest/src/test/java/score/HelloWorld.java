/*
 * Copyright 2021 ICONLOOP Inc.
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

package score;

import score.annotation.External;

public class HelloWorld {
    private final String name;

    public HelloWorld(String name) {
        this.name = name;
    }

    @External(readonly=true)
    public String name() {
        return name;
    }

    @External(readonly=true)
    public Address getAddress() {
        return Context.getAddress();
    }

    @External(readonly=true)
    public Address getOwner() {
        return Context.getOwner();
    }

    @External(readonly=true)
    public long getBlockTimestamp() {
        return Context.getBlockTimestamp();
    }

    @External(readonly=true)
    public byte[] computeHash(String algorithm, byte[] data) {
        return Context.hash(algorithm, data);
    }
}
