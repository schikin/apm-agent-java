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
import co.elastic.apm.agent.tracer.Transaction;
import io.micronaut.core.propagation.PropagatedContextElement;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerHttpRequestContext;
import net.bytebuddy.asm.Advice;

import javax.annotation.Nullable;
import java.util.Arrays;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class PropagatedContextAdvice {
    private static final Logger logger = LoggerFactory.getLogger(PropagatedContextAdvice.class);

    @Advice.OnMethodEnter(inline = false)
    @Advice.AssignReturned.ToArguments({
        @Advice.AssignReturned.ToArguments.ToArgument(index = 0, value = 0, typing = DYNAMIC),
        @Advice.AssignReturned.ToArguments.ToArgument(index = 1, value = 1, typing = DYNAMIC)
    })
    @Nullable
    public static Object[] onExit(
        @Advice.Argument(0) PropagatedContextElement[] elements
    ) {
        boolean elasticContextFound = false;
        ServerHttpRequestContext requestContext = null;

        for(PropagatedContextElement e : elements) {
            if(e instanceof ServerHttpRequestContext) {
                requestContext = (ServerHttpRequestContext) e;
            }

            if(e instanceof ElasticPropagatedContextElement) {
                elasticContextFound = true;
            }
        }

        if(!elasticContextFound && requestContext != null) {
            HttpRequest<?> request = requestContext.httpRequest();

            if (request == null) {
                return null;
            }

            Transaction<?> trx = HttpRequestUtil.findTransaction(request);

            if(trx == null) {
                return null;
            }

            int length = elements.length;

            elements = Arrays.copyOf(elements, length + 1);

            elements[length] = new ElasticPropagatedContextElement(trx);

            Object[] ret = new Object[] { elements, true };

            return ret;
        }

        return null;
    }
}
