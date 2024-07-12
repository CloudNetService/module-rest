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

import eu.cloudnetservice.ext.rest.api.HttpContext;
import java.util.Set;
import lombok.NonNull;

/**
 * A provider to authenticate a rest user based on the given http context. Some providers also allow to construct an
 * authentication token for a user (for example OAuth) while other providers can indicate that they are unable to do so
 * (for example basic auth - to create the encoded information the password must be available in plaintext which should
 * never be the case).
 * <p>
 * Provider discovery is done via service loading once per jvm lifetime. Note that only providers are discovered that
 * are defined in the same class loader that this provider class is loaded in.
 *
 * @since 1.0
 */
public interface AuthProvider {

  /**
   * The default provider priority that any base implementation should use.
   */
  int DEFAULT_PRIORITY = 0;

  /**
   * The priority of this provider. During registration providers with a higher priority will override providers with
   * the same name but a lower priority. In case the same priority is used by two providers with the same name, the
   * handling falls back to natural sorting.
   *
   * @return the priority to use for this provider during registration.
   * @see #DEFAULT_PRIORITY
   */
  int priority();

  /**
   * Gets if this provider supports the generation of authentication tokens. If this method returns false, the
   * {@link #generateAuthToken(RestUserManagement, RestUser, Set)} method cannot be called.
   *
   * @return true if this provider supports token generation, false otherwise.
   */
  boolean supportsTokenGeneration();

  /**
   * Get the name of this provider. Internally the result of this method is always converted to lower case, no other
   * constraints apply.
   *
   * @return the name of this auth provider.
   */
  @NonNull
  String name();

  /**
   * Generates a new authentication token for the given user. The returned auth token type is implementation dependant
   * and can be down-casted to get all the exposed information from the token.
   * <p>
   * The provided scopes should be included in the generated token, but only if the user has the requested scopes
   * assigned to himself. When doing further requests with the generated token the provider should ensure that both the
   * token and the user have the scope that is needed for the request.
   *
   * @param management the user management that was used to resolve the given rest user.
   * @param restUser   the rest user to generate an authentication token for.
   * @param scopes     the scopes that are requested by the user to be included in the auth token.
   * @return the generated authentication token for the given user.
   * @throws NullPointerException          if the given management or user is null.
   * @throws UnsupportedOperationException if this provider does not support token generation.
   * @see #supportsTokenGeneration()
   */
  @NonNull
  AuthTokenGenerationResult generateAuthToken(
    @NonNull RestUserManagement management,
    @NonNull RestUser restUser,
    @NonNull Set<String> scopes);

  /**
   * Tries to authenticate a user based on the given information from the client (through the http request that is
   * available from the http context) and the user management to resolve the requested user. It is required for this
   * method to perform token checks directly. If this method returns a successful authentication result it is assumed
   * that the provided user credentials did match the ones for a registered user in the given management.
   * <p>
   * This method should not throw any exception (except for the documented ones) and only indicate the handling state
   * via the returned authentication result.
   *
   * @param context        the http context information to get required information to authenticate a user.
   * @param management     the user management that should be used to load the requested user from.
   * @param requiredScopes the scopes that are required by the handler which called this auth provider.
   * @return an authentication result indicating the state to which this provider was able to handle the request.
   * @throws NullPointerException if the given http context, user management or the required scopes are null.
   */
  @NonNull
  AuthenticationResult tryAuthenticate(
    @NonNull HttpContext context,
    @NonNull RestUserManagement management,
    @NonNull Set<String> requiredScopes
  );
}
