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

import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface RestUserManagement {

  /**
   * Gets the rest user associated with the given id. The rest user must have been created previously using
   * {@link #saveRestUser(RestUser)}.
   * <p>
   * The rest user is always retrieved from the database, there is no caching.
   *
   * @param id the user id of the rest user to get from the database.
   * @return the rest user with the given id or null if there is no rest user.
   * @throws NullPointerException if the given id is null.
   */
  @Nullable
  RestUser restUser(@NonNull UUID id);

  @Nullable
  RestUser restUserByUsername(@NonNull String username);

  @NonNull
  @UnmodifiableView
  Collection<RestUser> users();

  /**
   * Creates and updates the given rest user. If the given user does not exist the user is created and saved otherwise
   * we update the users information in the database.
   *
   * @param user the user to save to the database.
   * @throws NullPointerException if the given user is null.
   */
  void saveRestUser(@NonNull RestUser user);

  /**
   * Deletes the given rest user from the database with immediate effect.
   *
   * @param id the id of the user to deleted from the database.
   * @return true if the user was deleted, false otherwise.
   * @throws NullPointerException if the given user is null.
   */
  boolean deleteRestUser(@NonNull UUID id);

  /**
   * Gets a new rest user builder.
   *
   * @return a new rest user builder.
   */
  @NonNull
  RestUser.Builder builder();

  /**
   * Gets a new rest user builder copying all properties from the given rest user into the new builder.
   *
   * @param restUser the user to copy all properties from.
   * @return a new rest user builder copying everything from the given user.
   * @throws NullPointerException if the given rest user is null.
   */
  @NonNull
  RestUser.Builder builder(@NonNull RestUser restUser);
}
