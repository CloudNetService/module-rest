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

import eu.cloudnetservice.ext.rest.api.auth.AuthToken;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

public record JwtAuthToken(
  @NonNull Set<String> scopes,
  @NonNull Instant creationTime,
  @NonNull JwtTokenHolder accessToken,
  @NonNull JwtTokenHolder refreshToken
) implements AuthToken<Map<String, Object>> {

  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    return JsonResponse.<Map<String, Object>>builder()
      .body(Map.of(
        "scopes", this.scopes,
        "accessToken", this.accessToken.serialize(),
        "refreshToken", this.refreshToken.serialize(),
        "creationTime", this.creationTime.toEpochMilli()));
  }
}
