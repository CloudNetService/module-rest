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

package eu.cloudnetservice.ext.rest.api.connection;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;

/**
 * A http connection info resolver trying to get more accurate connection information from a http request.
 * <ul>
 *   <li>Use the {@link eu.cloudnetservice.ext.rest.api.connection.parse.ForwardedSyntaxConnectionInfoResolver}
 *   for the {@link com.google.common.net.HttpHeaders#FORWARDED} header</li>
 *   <li>Use the {@link eu.cloudnetservice.ext.rest.api.connection.parse.HostHeaderConnectionInfoResolver}
 *   for the {@link com.google.common.net.HttpHeaders#HOST} header</li>
 *   <li>Use the {@link eu.cloudnetservice.ext.rest.api.connection.parse.XForwardSyntaxConnectionInfoResolver}
 *   for all {@code X-FORWARDED} headers</li>
 * </ul>
 *
 * @since 1.0
 */
@FunctionalInterface
public interface HttpConnectionInfoResolver {

  /**
   * Extracts more information about a http connection that might be given in the request using http headers.
   *
   * @param context  the context of the request that is processed.
   * @param baseInfo the base info resolved from the connection itself.
   * @return the extracted connection info on top of the already present one.
   * @throws NullPointerException if the given context or base info is null.
   */
  @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo);

  /**
   * Applies the given connection info resolver after this connection info extracted information. The newly extracted
   * information are passed into the next resolver.
   *
   * @param next the next resolver to apply after this one.
   * @return a new connection resolver applying the next resolver.
   * @throws NullPointerException if the given connection info resolver is null.
   */
  @CheckReturnValue
  default @NonNull HttpConnectionInfoResolver then(@NonNull HttpConnectionInfoResolver next) {
    return (context, baseInfo) -> {
      var replacedBaseInfo = this.extractConnectionInfo(context, baseInfo);
      return next.extractConnectionInfo(context, replacedBaseInfo);
    };
  }
}
