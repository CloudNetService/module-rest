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

import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Represents a http message which got sent from a client to the server.
 *
 * @since 1.0
 */
public interface HttpRequest extends HttpMessage<HttpRequest> {

  /**
   * The path parameters parsed from the request. For example the path {@code /docs/{topic}/index/{page}} would contain
   * the path parameters topic and page, parsed from the request uri before calling the request handler.
   *
   * @return all parsed path parameters for the current request.
   */
  @NonNull Map<String, String> pathParameters();

  /**
   * Gets the full, originally requested path.
   *
   * @return the full, originally requested path.
   */
  @NonNull String path();

  /**
   * Gets the full requested uri.
   *
   * @return the full requested uri.
   */
  @NonNull String uri();

  /**
   * Gets the method used for this request. The method might be one of
   * <ul>
   *   <li>OPTIONS
   *   <li>GET
   *   <li>HEAD
   *   <li>POST
   *   <li>PUT
   *   <li>PATCH
   *   <li>DELETE
   *   <li>TRACE
   *   <li>CONNECT
   * </ul>
   * <p>
   * See the <a href="https://developer.mozilla.org/de/docs/Web/HTTP/Methods">mdn</a> documentation for more information
   * about http request methods.
   *
   * @return the request method of the request.
   */
  @NonNull String method();

  /**
   * Gets all query parameters mapped by the key of it to the value. Each query parameter can have multiple values set.
   * The maximum amount of query parameters which will get decoded for a request are 1024.
   *
   * @return the query parameters supplied in the request uri.
   */
  @NonNull Map<String, List<String>> queryParameters();
}
