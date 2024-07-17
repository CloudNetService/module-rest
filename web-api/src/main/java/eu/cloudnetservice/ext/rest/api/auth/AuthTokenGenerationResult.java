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

package eu.cloudnetservice.ext.rest.api.auth;

import lombok.NonNull;

/**
 * The result of the auth token generation process provided by {@link AuthProvider}.
 *
 * @since 1.0
 */
public sealed interface AuthTokenGenerationResult permits
  AuthTokenGenerationResult.Success,
  AuthTokenGenerationResult.Constant {

  /**
   * Successful auth token generation result containing the newly generated token.
   *
   * @param authToken the newly generated auth token.
   * @param <T>       the type used for serialization of the token.
   */
  record Success<T>(@NonNull AuthToken<T> authToken) implements AuthTokenGenerationResult {

  }

  /**
   * A collection of jvm-static auth token generation results.
   */
  enum Constant implements AuthTokenGenerationResult {
    /**
     * The user requested scopes for the token that are not assigned to himself.
     */
    REQUESTED_INVALID_SCOPES,
  }
}
