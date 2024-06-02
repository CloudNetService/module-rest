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

import java.time.OffsetDateTime;
import lombok.NonNull;

/**
 * An audited user contains all information about the creation and modification of an entity.
 */
public interface AuditedUser {

  /**
   * Gets the offset date time the audited user was created at.
   *
   * @return the offset date time the audited user was created at.
   */
  @NonNull
  OffsetDateTime createdAt();

  /**
   * Gets the username of the user that created the audited entity. This might contain names, that do not correspond to
   * registered entities.
   *
   * @return the username of the user that created the audited entity.
   */
  @NonNull
  String createdBy();

  /**
   * Gets the offset date time at which the user was last modified. If the user was not modified the creation date time
   * is returned.
   *
   * @return the offset date time at which the user was last modified.
   */
  @NonNull
  OffsetDateTime modifiedAt();

  /**
   * Gets the username of the user that modified the audited entity. This might contain names, that do not correspond to
   * registered entities. If the user was not modified the creation username is returned.
   *
   * @return the username of the user that modified the audited entity.
   */
  @NonNull
  String modifiedBy();

  interface Builder {

    @NonNull
    Builder createdAt(@NonNull OffsetDateTime createdAt);

    @NonNull
    Builder createdBy(@NonNull String createdBy);

    @NonNull
    Builder modifiedAt(@NonNull OffsetDateTime modifiedAt);

    @NonNull
    Builder modifiedBy(@NonNull String modifiedBy);

    @NonNull
    AuditedUser build();
  }
}
