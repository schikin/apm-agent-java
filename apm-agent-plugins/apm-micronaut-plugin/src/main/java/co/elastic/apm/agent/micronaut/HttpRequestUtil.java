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
import co.elastic.apm.agent.tracer.GlobalTracer;
import co.elastic.apm.agent.tracer.Transaction;
import co.elastic.apm.agent.tracer.dispatch.AbstractHeaderGetter;
import co.elastic.apm.agent.tracer.dispatch.TextHeaderGetter;
import co.elastic.apm.agent.tracer.metadata.Request;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;

import javax.annotation.Nullable;

public class HttpRequestUtil {

    public static class HeaderGetter extends AbstractHeaderGetter<String, HttpHeaders> implements TextHeaderGetter<HttpHeaders> {

        @Nullable
        @Override
        public String getFirstHeader(String headerName, HttpHeaders carrier) {
            return carrier.get(headerName);
        }
    }

    public static final String REQUEST_ATTRIBUTE = "elastic.apm.transaction";

    public static void addTransaction(HttpRequest<?> httpRequest) {
        if(findTransaction(httpRequest) != null) {
            return;
        }

        Transaction<?> trx = GlobalTracer.get().startChildTransaction(httpRequest.getHeaders(), new HeaderGetter(), Thread.currentThread().getContextClassLoader());

        if(trx == null) {
            return;
        }

        trx.setFrameworkName("micronaut");
        trx.withType("request");

        StringBuilder nameBuilder = trx.getAndOverrideName(AbstractSpan.PRIORITY_LOW_LEVEL_FRAMEWORK);

        if(nameBuilder == null) {
            trx.end();
            return;
        }

        nameBuilder.append(httpRequest.getMethodName());
        nameBuilder.append(" ");
        nameBuilder.append(httpRequest.getUri().getPath());

        Request elasticRequestMetadata = trx.getContext().getRequest();

        elasticRequestMetadata.getSocket()
            .withRemoteAddress(httpRequest.getRemoteAddress().getAddress().getHostAddress());

        elasticRequestMetadata.withHttpVersion(httpRequest.getHttpVersion().name())
            .withMethod(httpRequest.getMethodName());

        elasticRequestMetadata.getUrl()
            .withProtocol("http")
            .withHostname(httpRequest.getRemoteAddress().getAddress().getHostAddress())
            .withPort(httpRequest.getServerAddress().getPort())
            .withPathname(httpRequest.getUri().getPath())
            .withSearch(httpRequest.getUri().getQuery());

        httpRequest.setAttribute(REQUEST_ATTRIBUTE, trx);
    }

    @Nullable
    public static Transaction<?> findTransaction(HttpRequest<?> request) {
        Object ret = request.getAttribute(REQUEST_ATTRIBUTE).orElse(null);

        if(ret instanceof Transaction<?>) {
            return (Transaction<?>) ret;
        } else {
            return null;
        }
    }
}
