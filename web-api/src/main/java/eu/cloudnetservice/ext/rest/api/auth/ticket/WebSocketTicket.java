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

package eu.cloudnetservice.ext.rest.api.auth.ticket;

import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import java.time.Instant;
import java.util.Set;
import lombok.NonNull;

public interface WebSocketTicket<T> extends IntoResponse<T> {

  @NonNull
  Instant expiresAt();

  @NonNull
  Set<String> scopes();

  @NonNull
  String tokenRepresentation();
}
