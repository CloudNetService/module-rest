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

package eu.cloudnetservice.ext.rest.api;

import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import lombok.NonNull;

/**
 * The http handler class is designed to handle incoming http requests and create the corresponding responses to the
 * requests. Each handler is responsible for the previously registered path and http request method.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface HttpHandler {

  /**
   * Handles a http request whose path (and other supplied attributes) while registering is matching the requested path
   * of the client. The request is only posted to the one handler that matches the requested path.
   *
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  @NonNull IntoResponse<?> handle(@NonNull HttpContext context) throws Exception;
}
