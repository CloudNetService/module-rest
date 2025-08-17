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
import eu.cloudnetservice.ext.rest.api.annotation.RequestBody;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

/**
 * A processor for the {@code @RequestBody} annotation.
 *
 * @since 1.0
 */
public final class RequestBodyProcessor implements HttpAnnotationProcessor {

  private static final Class<?>[] ALLOWED_BODY_TYPES = new Class<?>[]{
    byte[].class,
    String.class,
    Reader.class,
    ByteBuffer.class,
    InputStream.class,
  };

  /**
   * Checks if the given parameter type is one of the supported parameter types for the request body.
   *
   * @param type the type of the parameter.
   * @return true if the parameter type is supported, false otherwise.
   * @throws NullPointerException if the given type is null.
   */
  private static boolean usesSupportedParamType(@NonNull Class<?> type) {
    for (var allowedBodyType : ALLOWED_BODY_TYPES) {
      if (allowedBodyType.isAssignableFrom(type)) {
        return true;
      }
    }

    return false;
  }

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
      RequestBody.class,
      (param, annotation) -> {
        // check if any supported param type is used
        var paramType = param.getType();
        if (!usesSupportedParamType(paramType)) {
          throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
            .parameter(param)
            .handlerMethod(method)
            .annotationType(RequestBody.class)
            .debugDescription("Body type " + paramType.getSimpleName() + " is not supported")
            .build();
        }

        return (context) -> {
          if (String.class.isAssignableFrom(param.getType())) {
            return context.request().bodyAsString();
          } else if (byte[].class.isAssignableFrom(param.getType())) {
            return context.request().body();
          } else if (ByteBuffer.class.isAssignableFrom(param.getType())) {
            return ByteBuffer.wrap(context.request().body());
          } else if (InputStream.class.isAssignableFrom(param.getType())) {
            return context.request().bodyStream();
          } else if (Reader.class.isAssignableFrom(param.getType())) {
            return new InputStreamReader(context.request().bodyStream(), StandardCharsets.UTF_8);
          } else {
            // reachability fence: should not happen
            throw new IllegalStateException("Invalid body type that wasn't caught before: " + paramType);
          }
        };
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
