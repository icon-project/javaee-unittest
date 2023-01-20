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

/**
 * ManualRevertException is thrown by {@link score.Context} with user code.
 * <p>
 *     It's not catchable in the same context in the engine.
 *     To simulate this behavior, it doesn't inherit {@link score.RevertedException}.
 *     It's converted to {@link score.UserRevertedException} on returning to the caller.
 * </p>
 */
public class ManualRevertException extends RuntimeException {
    private int code;

    public int getCode() {
        return code;
    }

    public ManualRevertException() {
        super();
        code = 0;
    }

    public ManualRevertException(String msg) {
        super(msg);
        code = 0;
    }

    public ManualRevertException(String msg, Throwable cause) {
        super(msg, cause);
        code = 0;
    }

    public ManualRevertException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
