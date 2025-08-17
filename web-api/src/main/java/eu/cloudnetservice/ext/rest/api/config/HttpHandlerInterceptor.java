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

package eu.cloudnetservice.ext.rest.api.config;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.response.Response;
import lombok.NonNull;

/**
 * A preprocessor for a http context which is passed to a http handler.
 *
 * @since 1.0
 */
public interface HttpHandlerInterceptor {

  /**
   * Called to preprocess the given context before passing it to the associated handler. If this processor returns null,
   * it indicates that the http request processing should be stopped. The returned context is only passed to the
   * associated handlers, further invocations will not receive a new context instance returned by this method.
   *
   * @param context the current context of the request.
   * @return the http context to pass to the handler, or null to drop the request execution.
   * @throws NullPointerException if the given path or context is null.
   */
  default boolean preProcess(
    @NonNull HttpContext context,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config
  ) throws Exception {
    return true;
  }

  default boolean postProcess(
    @NonNull HttpContext context,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config,
    @NonNull Response<?> response
  ) throws Exception {
    return true;
  }

  default void postProcessExceptionally(
    @NonNull HttpContext context,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config,
    @NonNull Throwable exception
  ) throws Exception {
  }
}
