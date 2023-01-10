/*
 * Copyright 2020 ICON Foundation
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

package score.impl.struct;

import java.lang.reflect.Method;

public class MethodProperty implements Property {
    protected final Method method;

    public MethodProperty(Method method) {
        this.method = method;
    }

    @Override
    public String getName() {
        var name = method.getName();
        var pre = name.startsWith(kBooleanGetterPrefix) ? kBooleanGetterLength : kNormalGetterLength;
        return Property.decapitalize(name.substring(pre));
    }

    @Override
    public Class<?> getType() {
        return method.getParameterTypes()[0];
    }
}
