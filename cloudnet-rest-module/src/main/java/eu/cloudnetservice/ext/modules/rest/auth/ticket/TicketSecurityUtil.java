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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import lombok.NonNull;

public final class TicketSecurityUtil {

  private static final Path WS_TICKET_SECRET_PATH = Path.of("ws_ticket_secret");
  private static final HashFunction HMAC_SHA256 = hmacSha256();

  private TicketSecurityUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String generateTicket(@NonNull String data) {
    var base64 = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    var signature = generateTicketSignature(base64);
    return base64 + '.' + signature;
  }

  public static boolean verifyTicketSignature(@NonNull String data) {
    var ticketParts = data.split(":", 2);
    if (ticketParts.length != 2) {
      return false;
    }

    var base64Data = ticketParts[0];
    var signature = ticketParts[1];
    return generateTicketSignature(base64Data).equals(signature);
  }

  private static @NonNull String generateTicketSignature(@NonNull String base64Data) {
    return HMAC_SHA256.hashString(base64Data, StandardCharsets.UTF_8).toString();
  }

  private static @NonNull HashFunction hmacSha256() {
    try {
      if (Files.notExists(WS_TICKET_SECRET_PATH)) {
        var keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        var secretKey = keyGenerator.generateKey().getEncoded();
        Files.write(WS_TICKET_SECRET_PATH, secretKey);
      }

      return Hashing.hmacSha256(Files.readAllBytes(WS_TICKET_SECRET_PATH));
    } catch (IOException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to generate HmacSHA256 websocket signature validation key.", exception);
    }
  }
}
