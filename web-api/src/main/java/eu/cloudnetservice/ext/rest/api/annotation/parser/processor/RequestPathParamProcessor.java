/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.ext.rest.api.annotation.parser.processor;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import eu.cloudnetservice.ext.rest.api.problem.StandardProblemDetail;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A processor for the {@code @RequestPathParam} annotation.
 *
 * @since 1.0
 */
public final class RequestPathParamProcessor implements HttpAnnotationProcessor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handler
  ) {
    var hints = HttpAnnotationProcessorUtil.mapParameters(
      method,
      RequestPathParam.class,
      (param, annotation) -> (context) -> {
        // get the path parameter and error out if no value is present but the parameter is required
        var pathParam = context.request().pathParameters().get(annotation.value());
        if (pathParam == null) {
          throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
            .parameter(param)
            .handlerMethod(method)
            .annotationType(RequestPathParam.class)
            .debugDescription("Required path parameter " + annotation.value() + " is not registered. This is an error!")
            .build();
        }

        // set the path parameter in the context
        return pathParam;
      });
    config.addHandlerInterceptor(new HttpHandlerInterceptor() {
      @Override
      public boolean preProcess(
        @NonNull HttpContext context,
        @NonNull HttpHandler handler,
        @NonNull HttpHandlerConfig config
      ) {
        context.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
        return true;
      }
    });
  }
}
