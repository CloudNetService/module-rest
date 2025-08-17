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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

public record JwtTokenHolder(
  @NonNull String token,
  @NonNull String tokenId,
  @NonNull Instant expiresAt,
  @NonNull String tokenType
) {

  public static final String ACCESS_TOKEN_TYPE = "access";
  public static final String REFRESH_TOKEN_TYPE = "refresh";

  @Unmodifiable
  public @NonNull Map<String, Object> serialize() {
    var expiresIn = Duration.between(Instant.now(), this.expiresAt).toMillis();
    return Map.of(
      "token", this.token,
      "tokenType", this.tokenType,
      "expiresIn", expiresIn);
  }
}
