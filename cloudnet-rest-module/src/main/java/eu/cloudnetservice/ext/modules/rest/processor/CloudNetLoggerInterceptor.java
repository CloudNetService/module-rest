/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.modules.rest.processor;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import java.lang.reflect.Method;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudNetLoggerInterceptor implements HttpAnnotationProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetLoggerInterceptor.class);

  @Override
  public void buildPreprocessor(
    HttpHandlerConfig.@NonNull Builder config,
    @NonNull Method method,
    @NonNull Object handlerInstance
  ) {
    config.addHandlerInterceptor(new HttpHandlerInterceptor() {
      @Override
      public void postProcessExceptionally(
        @NonNull HttpContext context,
        @NonNull HttpHandler handler,
        @NonNull HttpHandlerConfig config,
        @NonNull Throwable exception
      ) {
        LOGGER.error("Exception occurred while processing annotations for {}", context.request().path(), exception);
        // in this case we want to respond with 500
        context.response().status(HttpResponseCode.INTERNAL_SERVER_ERROR);
      }
    });
  }
}
