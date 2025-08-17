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

/**
 * An empty connection info resolver that does no resolving.
 *
 * @since 1.0
 */
public final class EmptyConnectionInfoResolver implements HttpConnectionInfoResolver {

  public static final HttpConnectionInfoResolver INSTANCE = new EmptyConnectionInfoResolver();

  private EmptyConnectionInfoResolver() {
  }

  /**
   * An empty resolver just returning the given base info.
   *
   * @param context  the context of the request that is processed.
   * @param baseInfo the base info resolved from the connection itself.
   * @return the same base info as given.
   * @throws NullPointerException if the given context or base info is null.
   */
  @Override
  public @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo
  ) {
    return baseInfo;
  }
}
