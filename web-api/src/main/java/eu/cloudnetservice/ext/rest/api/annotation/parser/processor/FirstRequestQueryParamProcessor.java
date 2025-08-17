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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import java.lang.reflect.Method;
import java.net.URI;
import lombok.NonNull;

/**
 * A processor for the {@code @FirstRequestQueryParam} annotation.
 *
 * @since 1.0
 */
public final class FirstRequestQueryParamProcessor implements HttpAnnotationProcessor {

  private static final ProblemDetail MISSING_QUERY_PARAM_BASE_PROBLEM = ProblemDetail.builder()
    .status(HttpResponseCode.BAD_REQUEST)
    .type(URI.create("missing-query-param"))
    .title("Required Query Parameter Is Missing")
    .build();

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
      FirstRequestQueryParam.class,
      (param, annotation) -> (context) -> {
        // get the parameters and error out if no values are present but the parameter is required
        var queryParameters = context.request().queryParameters().get(annotation.value());
        if (!param.isAnnotationPresent(Optional.class) && (queryParameters == null || queryParameters.isEmpty())) {
          var problem = ProblemDetail.builder(MISSING_QUERY_PARAM_BASE_PROBLEM)
            .detail("Required query parameter " + annotation.value() + " is missing")
            .build();
          throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(problem)
            .parameter(param)
            .handlerMethod(method)
            .annotationType(FirstRequestQueryParam.class)
            .debugDescription("Missing required query parameter is missing: " + annotation.value())
            .build();
        }

        // return the first value or null if not possible
        return DefaultHttpAnnotationParser.applyDefault(
          annotation.def(),
          queryParameters == null ? null : Iterables.getFirst(queryParameters, null));
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
