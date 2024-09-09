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

import co.elastic.apm.agent.tracer.AbstractSpan;
import co.elastic.apm.agent.tracer.Transaction;
import io.micronaut.http.HttpRequest;
import io.micronaut.web.router.MethodBasedRouteInfo;
import io.micronaut.web.router.RouteInfo;
import io.micronaut.web.router.RouteMatch;
import net.bytebuddy.asm.Advice;

import javax.annotation.Nullable;

public class RouteExecutorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
    public static void onEnter(
        @Nullable @Advice.Argument(1) RouteMatch<?> match,
        @Nullable @Advice.Argument(2) HttpRequest<?> request) {

        if(request == null || match == null) {
            return;
        }

        Transaction<?> trx = HttpRequestUtil.findTransaction(request);

        if(trx == null) {
            return;
        }

        RouteInfo<?> routeInfo = match.getRouteInfo();

        if(routeInfo instanceof MethodBasedRouteInfo) {
            MethodBasedRouteInfo<?,?> methodBasedRouteInfo = (MethodBasedRouteInfo<?,?>) routeInfo;

            String controllerName = methodBasedRouteInfo.getTargetMethod().getDeclaringType().getSimpleName();
            String methodName = methodBasedRouteInfo.getTargetMethod().getName();

            StringBuilder nameBuilder = trx.getAndOverrideName(AbstractSpan.PRIORITY_LOW_LEVEL_FRAMEWORK);

            if(nameBuilder == null) {
                return;
            }

            nameBuilder.append(controllerName);
            nameBuilder.append("#");
            nameBuilder.append(methodName);

        }
    }
}
