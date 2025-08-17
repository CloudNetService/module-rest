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

import java.util.Locale;
import lombok.NonNull;

/**
 * Represents a http request method. Supporting the all request methods except {@code CONNECT} from the <a
 * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods">specification</a>.
 *
 * @since 1.0
 */
public enum HttpMethod {

  GET,
  PUT,
  POST,
  HEAD,
  PATCH,
  TRACE,
  DELETE,
  OPTIONS;

  /**
   * Getss the corresponding http method for the given input string.
   *
   * @param name the name of the http method.
   * @return the http method with the corresponding name.
   * @throws IllegalStateException if an unknown method is requested.
   * @throws NullPointerException  if the given name is null.
   */
  public static @NonNull HttpMethod fromName(@NonNull String name) {
    return switch (name.toUpperCase(Locale.ROOT)) {
      case "GET" -> GET;
      case "PUT" -> PUT;
      case "POST" -> POST;
      case "HEAD" -> HEAD;
      case "PATCH" -> PATCH;
      case "TRACE" -> TRACE;
      case "DELETE" -> DELETE;
      case "OPTIONS" -> OPTIONS;
      default -> throw new IllegalArgumentException("Unexpected http method name: " + name);
    };
  }
}
