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

public record ScopedRestUserDelegate(@NonNull RestUser delegate, @NonNull Set<String> scopes) implements RestUser {

  @Override
  public @NonNull UUID id() {
    return this.delegate.id();
  }

  @Override
  public @NonNull String username() {
    return this.delegate.username();
  }

  @Override
  public @NonNull OffsetDateTime createdAt() {
    return this.delegate.createdAt();
  }

  @Override
  public @NonNull String createdBy() {
    return this.delegate.createdBy();
  }

  @Override
  public @NonNull OffsetDateTime modifiedAt() {
    return this.delegate.modifiedAt();
  }

  @Override
  public @NonNull String modifiedBy() {
    return this.delegate.modifiedBy();
  }

  @Override
  public @Unmodifiable @NonNull Map<String, String> properties() {
    return this.delegate.properties();
  }

  @Override
  public boolean hasScope(@NonNull String scope) {
    return (this.scopes.isEmpty() || this.scopes.contains(scope)) && this.delegate.hasScope(scope);
  }

  @Override
  public @Unmodifiable @NonNull Set<String> scopes() {
    return Collections.unmodifiableSet(this.scopes);
  }

  @Override
  public Response.@NonNull Builder<Map<String, Object>, ?> intoResponseBuilder() {
    return this.delegate.intoResponseBuilder();
  }
}
