/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.ext.rest.jwt;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class JwtTokenPropertyParser {

  private static final String ENTRY_DELIMITER = ",";
  private static final String NEWLINE_DELIMITER = ";";

  private JwtTokenPropertyParser() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Collection<JwtTokenHolder> parseTokens(@Nullable String propertyValue) {
    if (propertyValue == null || propertyValue.isBlank()) {
      // property is not present, we can just return an empty list as there are no tokens
      // initial capacity is two here: it might be that someone wants to register a new key pair after parsing
      // and this only required a list with an initial size of 3 (to prevent a resize), not 10
      return new ArrayList<>(3);
    }

    // this uses a CSV format for storing the entries but for newlines we don't use \n and a semicolon instead
    // note: due to us using a one-char string with non-regex chars, split is not actually using a regex to split
    var lines = propertyValue.split(NEWLINE_DELIMITER);
    Collection<JwtTokenHolder> tokens = new ArrayList<>(lines.length);
    for (var tokenInfoLine : lines) {
      // each line uses the following format: id,tokenType,exp
      // this is a basic check to ensure that this is the case before continuing
      var tokenInfoParts = tokenInfoLine.split(ENTRY_DELIMITER, 4);
      if (tokenInfoParts.length != 3) {
        continue;
      }

      try {
        // parse expiration time & construct the final token
        var expiration = Instant.ofEpochMilli(Long.parseLong(tokenInfoParts[2]));
        var tokenInfo = new JwtTokenHolder("", tokenInfoParts[0], expiration, tokenInfoParts[1]);
        tokens.add(tokenInfo);
      } catch (DateTimeException | NumberFormatException ignored) {
        // if serialized correctly these cannot happen, therefore we just ignore it
      }
    }

    return tokens;
  }

  public static @Nullable String compactTokens(@NonNull Collection<JwtTokenHolder> tokens) {
    if (tokens.isEmpty()) {
      // no tokens, nothing to compact
      return null;
    } else {
      // formats to a CSV format for each entry (without the header line)
      return tokens.stream()
        .map(holder -> {
          // each line uses the following format: id,tokenType,exp
          var expirationMillis = Long.toString(holder.expiresAt().toEpochMilli());
          return String.join(ENTRY_DELIMITER, holder.tokenId(), holder.tokenType(), expirationMillis);
        })
        .collect(Collectors.joining(NEWLINE_DELIMITER));
    }
  }
}
