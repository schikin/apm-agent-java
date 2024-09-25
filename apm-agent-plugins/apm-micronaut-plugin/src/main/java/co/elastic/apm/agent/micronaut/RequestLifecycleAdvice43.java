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

import io.micronaut.core.execution.ExecutionFlow;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class RequestLifecycleAdvice43 {
    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
    @Advice.AssignReturned.ToReturned(typing = Assigner.Typing.DYNAMIC)
    @Nullable
    public static ExecutionFlow<MutableHttpResponse<?>> onExit(
        @Advice.Argument(value = 0, typing = Assigner.Typing.DYNAMIC) @Nullable HttpRequest<?> httpRequest,
        @Advice.Return @Nullable ExecutionFlow<MutableHttpResponse<?>> returnFlow,
        @Advice.Thrown @Nullable Throwable t) {
        return RequestLifeCycleAdviceHelper.onNormalFlowExit(httpRequest, returnFlow, t);
    }
}