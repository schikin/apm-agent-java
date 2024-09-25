/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.agent.micronaut;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

//instrument for Micronaut 4.0-4.3, based on the availability of of deprecated method
public class RequestLifecycleInstrumentation40 extends RequestLifeCycleInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return hasSuperType(named("io.micronaut.http.server.RequestLifecycle")).and(not(hasDeprecatedNormalFlow()));
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("normalFlow");
    }

    @Override
    public String getAdviceClassName() {
        return "co.elastic.apm.agent.micronaut.RequestLifecycleAdvice40";
    }
}