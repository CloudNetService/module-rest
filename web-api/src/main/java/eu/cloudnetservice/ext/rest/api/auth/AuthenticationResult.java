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

import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The result of an authentication process returned by an authentication provider.
 *
 * @since 1.0
 */
public sealed interface AuthenticationResult permits
  AuthenticationResult.Success,
  AuthenticationResult.Constant,
  AuthenticationResult.InvalidTokenType {

  /**
   * Collection of jvm-static authentication results that do not provide any details why the authentication failed,
   * other than the name of the constant.
   *
   * @since 1.0
   */
  enum Constant implements AuthenticationResult {

    /**
     * The current called handler couldn't find the required information to try the authentication process and the next
     * available handler should be used instead.
     */
    PROCEED,
    /**
     * The requested user was not found.
     */
    USER_NOT_FOUND,
    /**
     * The credentials that were provided to the auth provider where invalid and therefore the authentication process
     * couldn't be completed.
     */
    INVALID_CREDENTIALS,
    /**
     * The user does not have the scopes required by the handler.
     */
    MISSING_REQUIRED_SCOPES,
  }

  /**
   * A successful authentication result containing the authenticated subject.
   *
   * @param restUser the rest user that was successfully authenticated, not null.
   * @param tokenId  the id of the token used for auth, null in case the tokens have no id.
   * @since 1.0
   */
  record Success(@NonNull RestUser restUser, @Nullable String tokenId) implements AuthenticationResult {

  }

  /**
   * Indicates that the type of token that was supplied to the auth provider is invalid for the desired use, but would
   * be valid in some other context. This can for example happen when a valid JWT refresh token is used to try and
   * authenticate a user to use an endpoint.
   *
   * @param restUser  the user that was determined from the given token (that has the invalid type).
   * @param tokenId   the id of the token that was invalid, null in case the tokens have no id.
   * @param tokenType the type of token that was incorrectly supplied, null if the token type is unknown.
   * @since 1.0
   */
  record InvalidTokenType(
    @NonNull RestUser restUser,
    @NonNull Set<String> scopes,
    @Nullable String tokenId,
    @Nullable String tokenType
  ) implements AuthenticationResult {

  }
}
