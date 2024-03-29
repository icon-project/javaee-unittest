/*
 * Copyright 2023 PARAMETA Corp.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @GenerateTScore} is used to generate a wrapper SCORE class for testing.
 * If you deploy the generated SCORE class using the unittest framework,
 * the following additional features are available.
 * <ul>
 * <li>Log a proper event on calling the method tagged with {@link score.annotation.EventLog}</li>
 * <li>Check if a method is properly tagged with {@link score.annotation.External} and {@link score.annotation.Payable}</li>
 * <li>Check if a parameter is properly tagged with {@link score.annotation.Optional}</li>
 * </ul>
 * For this, the class and its methods should be non-final in order to be extended by the generated SCORE class.
 *
 * <p>For example, the following is your contract implementation.
 * <pre>
 * public class MyContract {
 *     public MyContract() { }
 *
 *     &#64;External
 *     public void setValue(String value) {
 *         ...
 *     }
 * }
 * </pre>
 *
 * Then you may deploy the contract with the generated SCORE class in the unittest.
 * Note that the default suffix for the generated class is <em>TS</em>.
 * <pre>
 * public class MyContractTest {
 *     &#64;Test
 *     &#64;GenerateTScore(MyContract.class)
 *     public void testSetValue() {
 *         var score = sm.deploy(MyContractTS.class);
 *     }
 * }
 * </pre>
 *
 * Of course, you may use the class as is, without any additional features.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(GenerateTScores.class)
public @interface GenerateTScore {
    Class<?> value() default GenerateTScore.class;
    String suffix() default "TS";
}
