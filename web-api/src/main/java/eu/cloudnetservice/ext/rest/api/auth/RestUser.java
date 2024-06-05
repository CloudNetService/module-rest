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

import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface RestUser extends AuditedUser, IntoResponse<Map<String, Object>> {

  /**
   * The global administration scope. A user that gets this scope granted has access to all resources.
   */
  String GLOBAL_ADMIN_SCOPE = "global:admin";

  // https://regex101.com/r/3nG0Nu/1

  /**
   * The regex that scope names need to conform to.
   */
  String SCOPE_NAMING_REGEX = "(^[a-z][a-z0-9_]{4,39}):([a-z0-9.\\-_]+)";

  /**
   * The regex that scope names need to conform to.
   */
  Pattern SCOPE_NAMING_PATTERN = Pattern.compile(SCOPE_NAMING_REGEX);

  /**
   * The regex that usernames need to conform to.
   */
  String USER_NAMING_REGEX = "^[a-zA-Z0-9]{4,16}";
  /**
   * The regex that usernames need to conform to.
   */
  Pattern USER_NAMING_PATTERN = Pattern.compile(USER_NAMING_REGEX);

  /**
   * Gets the unique id of this rest user. The unique id is used for database operations with the user.
   *
   * @return the unique id of this rest user.
   */
  @NonNull
  UUID id();

  /**
   * Gets the unique username of this rest user. The username is used for display and login purposes. The username has
   * to follow the {@link #USER_NAMING_PATTERN}.
   *
   * @return the unique username of this rest user.
   */
  @NonNull
  String username();

  @NonNull
  OffsetDateTime createdAt();

  @NonNull
  OffsetDateTime modifiedAt();

  @Unmodifiable
  @NonNull
  Map<String, String> properties();

  /**
   * Checks whether the user has the given scope.
   *
   * @param scope the scope to check.
   * @return true if the rest user has that scope assigned, false otherwise.
   * @throws NullPointerException if the given scope is null.
   */
  boolean hasScope(@NonNull String scope);

  /**
   * Checks whether the user has at least one of the given scopes.
   *
   * @param scopes the scopes to check.
   * @return true if the user has at least one of the given scopes.
   * @throws NullPointerException if the given scopes array is null.
   */
  default boolean hasOneScopeOf(@NonNull String[] scopes) {
    for (var scope : scopes) {
      if (this.hasScope(scope)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets an unmodifiable view of the scopes the rest user has.
   *
   * @return an unmodifiable view of the scopes the rest user has.
   */
  @Unmodifiable
  @NonNull
  Set<String> scopes();

  /**
   * The rest user builder used to create and modify rest users to ensure immutability of the rest user itself.
   * <p>
   * Obtain a fresh builder instance using {@link RestUserManagement#builder()} or
   * {@link RestUserManagement#builder(RestUser)} if you want to copy existing properties.
   *
   * @see RestUser
   * @see RestUserManagement
   * @since 4.0
   */
  interface Builder extends AuditedUser.Builder {

    @NonNull
    Builder username(@NonNull String username);

    @NonNull
    Builder property(@NonNull String key, @Nullable String value);

    @NonNull
    Builder properties(@NonNull Map<String, String> properties);

    @NonNull
    Builder modifyProperties(@NonNull Consumer<Map<String, String>> modifier);

    /* TODO: fix
     * Adds the given scope to the rest users scopes. The scope has to follow the
     * {@link RestUserManagement#SCOPE_NAMING_REGEX} regex pattern. The only exception to that is the {@link GLOBAL_ADMIN_SCOPE}
     * scope that grants access to everything.
     *
     * @param scope the scope to add to the rest users scope.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given scope is null.
     * @throws IllegalArgumentException if the scope does not follow the mentioned regex pattern.
     */
    @NonNull
    Builder scope(@NonNull String scope);

    @NonNull
    Builder scopes(@NonNull Collection<String> scopes);

    @NonNull
    Builder modifyScopes(@NonNull Consumer<Collection<String>> modifier);

    /**
     * Creates the rest user from this builder.
     *
     * @return the newly built rest user from this builder.
     * @throws NullPointerException if no id was set.
     */
    @NonNull
    RestUser build();
  }
}
