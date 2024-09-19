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

package eu.cloudnetservice.ext.modules.rest.config;

import java.time.Duration;
import lombok.NonNull;

public record AuthConfiguration(
  int jwtTokenLifetimeSeconds,
  int jwtRefreshTokenLifetimeSeconds,
  int ticketLifetimeSeconds
) {

  public static final AuthConfiguration DEFAULT_CONFIGURATION = new AuthConfiguration(
    12 * 60 * 60, // 12h
    3 * 24 * 60 * 60, // 3d
    15 // 15s
  );

  public void validate() {
    if (this.jwtTokenLifetimeSeconds <= 0
      || this.jwtRefreshTokenLifetimeSeconds <= 0
      || this.ticketLifetimeSeconds <= 0) {
      throw new IllegalStateException("invalid authentication configuration: one lifetime is less or equal to zero");
    }
  }

  public @NonNull Duration jwtTokenLifetime() {
    return Duration.ofSeconds(this.jwtTokenLifetimeSeconds);
  }

  public @NonNull Duration jwtRefreshTokenLifetime() {
    return Duration.ofSeconds(this.jwtRefreshTokenLifetimeSeconds);
  }

  public @NonNull Duration ticketLifetime() {
    return Duration.ofSeconds(this.ticketLifetimeSeconds);
  }
}
