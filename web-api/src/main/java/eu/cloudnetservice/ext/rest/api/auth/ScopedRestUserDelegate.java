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

import eu.cloudnetservice.ext.rest.api.response.Response;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A delegate of a rest user for scoped auth contexts. In scoped auth contexts it has to be ensured that the user only
 * has access to the scopes that were provided when requesting the used auth token.
 *
 * @param delegate the actual user behind the delegate.
 * @param scopes   the scopes from the auth token the user is allowed to use. Leave empty if all the users scopes are
 *                 allowed.
 * @since 1.0
 */
public record ScopedRestUserDelegate(@NonNull RestUser delegate, @NonNull Set<String> scopes) implements RestUser {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull UUID id() {
    return this.delegate.id();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String username() {
    return this.delegate.username();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull OffsetDateTime createdAt() {
    return this.delegate.createdAt();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String createdBy() {
    return this.delegate.createdBy();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull OffsetDateTime modifiedAt() {
    return this.delegate.modifiedAt();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String modifiedBy() {
    return this.delegate.modifiedBy();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Map<String, String> properties() {
    return this.delegate.properties();
  }

  /**
   * Checks if the user has the given scope. If the user is from a scoped auth context then this method will check both
   * the provided scopes from the auth token and the currently assigned scopes for the user.
   *
   * @param scope the scope to check.
   * @return true if the user has the given scope, false otherwise.
   * @throws NullPointerException if the given scope is null.
   */
  @Override
  public boolean hasScope(@NonNull String scope) {
    return (this.scopes.isEmpty() || this.scopes.contains(scope)) && this.delegate.hasScope(scope);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Set<String> scopes() {
    return Collections.unmodifiableSet(this.scopes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Response.@NonNull Builder<Map<String, Object>, ?> intoResponseBuilder() {
    return this.delegate.intoResponseBuilder();
  }
}
