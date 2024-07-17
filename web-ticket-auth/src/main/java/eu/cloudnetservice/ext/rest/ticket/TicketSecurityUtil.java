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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class TicketSecurityUtil {

  private TicketSecurityUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String signTicket(@NonNull Mac function, @NonNull String data) {
    var base64 = Base64.getUrlEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    var signature = generateTicketSignature(function, base64);
    return base64 + '.' + signature;
  }

  public static boolean verifyTicketSignature(@NonNull Mac function, @NonNull String data) {
    var ticketParts = data.split("\\.", 2);
    if (ticketParts.length != 2) {
      return false;
    }

    var base64Data = ticketParts[0];
    var signature = ticketParts[1];
    return generateTicketSignature(function, base64Data).equals(signature);
  }

  public static @Nullable String extractTicketInformation(@NonNull String ticket) {
    var ticketParts = ticket.split("\\.", 2);
    if (ticketParts.length != 2) {
      return null;
    }

    var data = Base64.getUrlDecoder().decode(ticketParts[0]);
    return new String(data, StandardCharsets.UTF_8);
  }

  private static @NonNull String generateTicketSignature(@NonNull Mac function, @NonNull String base64Data) {
    return HexFormat.of().formatHex(function.doFinal(base64Data.getBytes(StandardCharsets.UTF_8)));
  }
}
