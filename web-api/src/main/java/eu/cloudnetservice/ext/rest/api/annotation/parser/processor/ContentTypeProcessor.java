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

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.annotation.ContentType;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import eu.cloudnetservice.ext.rest.api.response.Response;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A processor for the {@code @ContentType} annotation.
 *
 * @since 1.0
 */
public final class ContentTypeProcessor implements HttpAnnotationProcessor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handler
  ) {
    config.addHandlerInterceptor(new HttpHandlerInterceptor() {
      @Override
      public boolean postProcess(
        @NonNull HttpContext context,
        @NonNull HttpHandler handler,
        @NonNull HttpHandlerConfig config,
        @NonNull Response<?> response
      ) {
        var annotation = method.getAnnotation(ContentType.class);
        if (annotation != null) {
          context.response().headers().setIfAbsent(HttpHeaders.CONTENT_TYPE, annotation.value());
        }

        return true;
      }
    });
  }
}
