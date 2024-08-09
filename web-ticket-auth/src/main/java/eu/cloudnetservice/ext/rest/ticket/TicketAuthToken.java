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

package eu.cloudnetservice.ext.rest.ticket;

import eu.cloudnetservice.ext.rest.api.auth.AuthToken;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record TicketAuthToken(
  @NonNull UUID userId,
  @NonNull Instant creationTime,
  @NonNull String token,
  @NonNull Set<String> scopes
) implements AuthToken<Map<String, Object>> {

  public static final String PROPERTY_DELIMITER = ":";
  public static final String SCOPE_DELIMITER = ";";

  public static @Nullable TicketAuthToken parseTicket(@NonNull String ticketSecret) {
    var ticketToken = TicketSecurityUtil.extractTicketInformation(ticketSecret);
    if (ticketToken == null) {
      return null;
    }

    var parts = ticketToken.split(PROPERTY_DELIMITER, 3);
    if (parts.length < 2) {
      return null;
    }

    try {
      var creationTime = Instant.ofEpochSecond(Long.parseLong(parts[0]));
      var userId = UUID.fromString(parts[1]);

      Set<String> scopes = Set.of();
      if (parts.length == 3) {
        scopes = Set.of(parts[2].split(SCOPE_DELIMITER));
      }

      return new TicketAuthToken(userId, creationTime, ticketToken, scopes);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    return JsonResponse.<Map<String, Object>>builder().body(Map.of(
      "creationTime", this.creationTime.toEpochMilli(),
      "secret", this.token,
      "scopes", this.scopes));
  }
}
