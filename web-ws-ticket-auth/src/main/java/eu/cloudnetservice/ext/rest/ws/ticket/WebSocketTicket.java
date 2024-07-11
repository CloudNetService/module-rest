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

package eu.cloudnetservice.ext.rest.ws.ticket;

import eu.cloudnetservice.ext.rest.api.auth.AuthToken;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record WebSocketTicket(
  @NonNull UUID userId,
  @NonNull Instant creationTime,
  @NonNull String token,
  @NonNull Set<String> scopes
) implements AuthToken<Map<String, Object>> {

  public static final String PROPERTY_DELIMITER = ":";
  public static final String SCOPE_DELIMITER = ";";

  public static @Nullable WebSocketTicket parseTicket(@NonNull String ticketToken) {
    var parts = ticketToken.split(PROPERTY_DELIMITER, 3);
    var millis = parts[0];

    try {
      var creationTime = Instant.ofEpochMilli(Long.parseLong(millis));
      var userId = UUID.fromString(parts[1]);

      Set<String> scopes = java.util.Set.of();
      if (parts.length == 3) {
        scopes = java.util.Set.of(parts[2].split(SCOPE_DELIMITER));
      }

      return new WebSocketTicket(userId, creationTime, ticketToken, scopes);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    return JsonResponse.<Map<String, Object>>builder().body(Map.of(
      "creationTime",
      this.creationTime.toEpochMilli(),
      "secret",
      this.token,
      "scopes",
      this.scopes));
  }
}
