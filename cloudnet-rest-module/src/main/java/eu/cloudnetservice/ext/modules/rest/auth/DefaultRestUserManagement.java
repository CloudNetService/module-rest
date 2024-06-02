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

package eu.cloudnetservice.ext.modules.rest.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public final class DefaultRestUserManagement implements RestUserManagement {

  private static final String REST_USER_DB_NAME = "cloudnet_rest_users";

  private final LocalDatabase localDatabase;
  private final LoadingCache<UUID, RestUser> restUserCache;

  public DefaultRestUserManagement() {
    this(InjectionLayer.ext().instance(NodeDatabaseProvider.class));
  }

  /**
   * Creates the default user management and initializes the used database.
   *
   * @param databaseProvider the node database provider to use to create the user database.
   */
  public DefaultRestUserManagement(@NonNull NodeDatabaseProvider databaseProvider) {
    this.localDatabase = databaseProvider.database(REST_USER_DB_NAME);
    this.restUserCache = Caffeine.newBuilder()
      .scheduler(Scheduler.systemScheduler())
      .expireAfterWrite(Duration.ofMinutes(5))
      .build(key -> {
        var userDocument = this.localDatabase.get(key.toString());
        return userDocument == null ? null : userDocument.toInstanceOf(DefaultRestUser.class);
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable RestUser restUser(@NonNull UUID id) {
    return this.restUserCache.get(id);
  }

  @Override
  public @Nullable RestUser restUserByUsername(@NonNull String username) {
    var cache = this.restUserCache.asMap();
    var user = cache.values().stream()
      .filter(restUser -> restUser.username().equals(username))
      .findAny()
      .orElse(null);

    // found the user in the local cache
    if (user != null) {
      return user;
    }

    user = this.localDatabase.find("username", username).stream()
      .findFirst()
      .map(document -> document.toInstanceOf(DefaultRestUser.class))
      .orElse(null);

    // user found in database, store in cache
    if (user != null) {
      this.restUserCache.put(user.id(), user);
    }

    return user;
  }

  @Override
  public @NonNull @UnmodifiableView Collection<RestUser> users() {
    return this.localDatabase.documents()
      .stream()
      .map(document -> document.toInstanceOf(DefaultRestUser.class))
      .map(user -> (RestUser) user)
      .peek(user -> this.restUserCache.put(user.id(), user))
      .toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveRestUser(@NonNull RestUser user) {
    this.restUserCache.put(user.id(), user);
    this.localDatabase.insert(user.id().toString(), DocumentFactory.json().newDocument(user));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteRestUser(@NonNull UUID id) {
    this.restUserCache.invalidate(id);
    return this.localDatabase.delete(id.toString());
  }

  @Override
  public @NonNull DefaultRestUser.Builder builder() {
    return new DefaultRestUser.Builder();
  }

  @Override
  public @NonNull DefaultRestUser.Builder builder(@NonNull RestUser restUser) {
    return this.builder()
      .id(restUser.id())
      .scopes(restUser.scopes())
      .username(restUser.username())
      .createdAt(restUser.createdAt())
      .createdBy(restUser.createdBy())
      .modifiedAt(restUser.modifiedAt())
      .modifiedBy(restUser.modifiedBy())
      .properties(restUser.properties());
  }
}
