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

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cookie which can be sent by a client to the server using the {@code Cookie} header, and vise-vera using
 * the {@code Set-Cookie} header. See the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies">mdn</a>
 * documentation about cookies for more details.
 *
 * @param name     the name of the cookie.
 * @param value    the value of the cookie.
 * @param domain   the domain which is allowed to access the cookie. If not set it defaults to the current domain,
 *                 excluding subdomains.
 * @param path     the path that must be present in the request uri in order to include this cookie in the header.
 * @param httpOnly sets that the cookie is invisible to scripts and will only be sent in http requests to the server.
 * @param secure   sets that the cookie is only sent to the server when the connection is encrypted (using https).
 * @param wrap     sets if the value of this cookie should be wrapped in double quotes.
 * @param maxAge   the maximum age until the cookie gets deleted from the client.
 * @since 1.0
 */
public record HttpCookie(
  @NonNull String name,
  @NonNull String value,
  @Nullable String domain,
  @Nullable String path,
  boolean httpOnly,
  boolean secure,
  boolean wrap,
  long maxAge
) {

  /**
   * Constructs a new http cookie instance. This constructor only takes the required values of a cookie.
   *
   * @param name  the name of the cookie.
   * @param value the value of the cookie.
   * @throws NullPointerException if either the given name or value is null.
   */
  public HttpCookie(@NonNull String name, @NonNull String value) {
    this(name, value, null, null, Long.MAX_VALUE);
  }

  /**
   * Constructs a new http cookie instance.
   *
   * @param name   the name of the cookie.
   * @param value  the value of the cookie.
   * @param domain the domain which is allowed to access the cookie. If not set it defaults to the current domain,
   *               excluding subdomains.
   * @param path   the path that must request in the request uri in order to include this cookie in the header.
   * @param maxAge the maximum age until the cookie gets deleted from the client.
   * @throws NullPointerException if either the given name or value is null.
   */
  public HttpCookie(
    @NonNull String name,
    @NonNull String value,
    @Nullable String domain,
    @Nullable String path,
    long maxAge
  ) {
    this(name, value, domain, path, false, false, false, maxAge);
  }
}
