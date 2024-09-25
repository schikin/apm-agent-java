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
import co.elastic.apm.agent.tracer.metadata.Response;
import co.elastic.apm.agent.tracer.util.ResultUtil;
import io.micronaut.core.execution.ExecutionFlow;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;

import javax.annotation.Nullable;

public class RequestLifeCycleAdviceHelper {
    private static final Logger logger = LoggerFactory.getLogger(RequestLifeCycleAdviceHelper.class);

    @Nullable
    public static ExecutionFlow<MutableHttpResponse<?>> onNormalFlowExit(
        @Nullable HttpRequest<?> httpRequest,
        @Nullable ExecutionFlow<MutableHttpResponse<?>> returnFlow,
        @Nullable Throwable t) {

        if(httpRequest == null) {
            logger.debug("normalFlow exit handler, request is null, this should not occur under normal circumstances");

            return null;
        }

        logger.debug("normalFlow exit handler, request: {}", httpRequest);

        Transaction<?> trx = HttpRequestUtil.findTransaction(httpRequest);

        if(trx == null) {
            return null;
        }

        if(t != null) {
            finishTransaction(trx, null, t);
            return null;
        }

        if (returnFlow == null) {
            return null;
        }

        return returnFlow.map((res) -> {
            finishTransaction(trx, res, null);
            return res;
        });

    }

    private static void finishTransaction(
        Transaction<?> trx,
        @Nullable MutableHttpResponse<?> response,
        @Nullable Throwable exception)
    {

        if(response != null) {
            logger.debug("finishing transaction, {}", response.toString());

            trx
                .withResultIfUnset(ResultUtil.getResultByHttpStatus(response.code()))
                .captureException(exception);

            Response trxResponse = trx.getContext().getResponse();

            trxResponse
                .withFinished(true)
                .withStatusCode(response.code());
        } else {
            logger.debug("finish transaction called with empty response");
        }

        trx.end();
    }
}
