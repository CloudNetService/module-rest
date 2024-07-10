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

package eu.cloudnetservice.ext.modules.rest.auth.ticket;

import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record WebSocketTicket(
  @NonNull Instant expiresAt,
  @NonNull Set<String> restrictions
) implements IntoResponse<Map<String, Object>> {

  public static final String PROPERTY_DELIMITER = ":";
  public static final String RESTRICTION_DELIMITER = ";";

  public static @NonNull WebSocketTicket of(@NonNull Set<String> restrictions) {
    return new WebSocketTicket(Instant.now().plus(30, ChronoUnit.SECONDS), restrictions);
  }

  public static @Nullable WebSocketTicket parseTicket(@NonNull String ticketRepresentation) {
    var parts = ticketRepresentation.split(":", 2);
    var millis = parts[0];

    try {
      Set<String> restrictions = Set.of();
      var expiresAt = Instant.ofEpochMilli(Long.parseLong(millis));
      if (parts.length == 2) {
        restrictions = Set.of(parts[1].split(RESTRICTION_DELIMITER));
      }

      return new WebSocketTicket(expiresAt, restrictions);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    var expiresAtMillis = this.expiresAt.toEpochMilli();
    var encodedTicket = TicketSecurityUtil.generateTicket(this.buildCompact());

    return JsonResponse.<Map<String, Object>>builder().body(Map.of(
      "expiresAt",
      expiresAtMillis,
      "secret",
      encodedTicket,
      "restrictions",
      this.restrictions));
  }

  public @NonNull String buildCompact() {
    if (this.restrictions.isEmpty()) {
      return Long.toString(this.expiresAt.toEpochMilli());
    }

    return this.expiresAt.toEpochMilli() + PROPERTY_DELIMITER + String.join(RESTRICTION_DELIMITER, this.restrictions);
  }
}
