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

import co.elastic.apm.agent.sdk.logging.Logger;
import co.elastic.apm.agent.sdk.logging.LoggerFactory;
import co.elastic.apm.agent.tracer.GlobalTracer;
import co.elastic.apm.agent.tracer.Transaction;
import co.elastic.apm.agent.tracer.dispatch.AbstractHeaderGetter;
import co.elastic.apm.agent.tracer.dispatch.TextHeaderGetter;
import com.sun.net.httpserver.Headers;
import io.micronaut.core.execution.ExecutionFlow;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import net.bytebuddy.asm.Advice;

import javax.annotation.Nullable;

public class RequestLifecycleAdvice {
    private static final Logger log = LoggerFactory.getLogger(RequestLifecycleAdvice.class);

    public static class HeaderGetter extends AbstractHeaderGetter<String, HttpHeaders> implements TextHeaderGetter<HttpHeaders> {

        @Nullable
        @Override
        public String getFirstHeader(String headerName, HttpHeaders carrier) {
            return carrier.get(headerName);
        }
    }

    @Nullable
    @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
    public static Object onEnter(
            @Advice.FieldValue("request") @Nullable Object requestUntyped
        ) {
        if(requestUntyped == null) {
            return null;
        }

        HttpRequest<?> typedRequest = (HttpRequest<?>) requestUntyped;

        Transaction<?> trx = GlobalTracer.get().startChildTransaction(typedRequest.getHeaders(), new HeaderGetter(), Thread.currentThread().getContextClassLoader());

        PropagatedContextElement elasticContextElement = new PropagatedContextElement(trx);

        PropagatedContext ctx = PropagatedContext.getOrEmpty().plus(elasticContextElement);

        return ctx.propagate();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
    public static void onExit(
        @Advice.Enter @Nullable Object scopeUntyped,
        @Advice.Return @Nullable ExecutionFlow<MutableHttpResponse<?>> returnFlow,
        @Advice.Thrown @Nullable Throwable t) {

        PropagatedContext.Scope scope = (PropagatedContext.Scope) scopeUntyped;

        if (scope == null) {
            return;
        }

        scope.close();

        if(t != null) {
            finishTransaction(null, t);
            return;
        }

        if (returnFlow == null) {
            return;
        }

        returnFlow.onComplete(RequestLifecycleAdvice::finishTransaction);
    }

    private static void finishTransaction(
        @Nullable MutableHttpResponse<?> response,
        @Nullable Throwable exception)
    {
        PropagatedContextElement context = PropagatedContext.getOrEmpty().get(PropagatedContextElement.class);

        if (context == null) {
            return;
        }

        Transaction<?> trx = context.getTransaction();

        if (exception != null) {
            trx.captureException(exception);
        }

        trx.end();
    }
}
