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

package eu.cloudnetservice.ext.modules.rest.dto;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.ext.modules.rest.validation.HostAddress;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public final class HostAndPortDto implements Dto<HostAndPort> {

  @NotNull
  @HostAddress
  private final String host;
  @Min(1)
  @Max(0xFFFF)
  private final int port;

  public HostAndPortDto(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public @NonNull HostAndPort toEntity() {
    return new HostAndPort(this.host, this.port);
  }
}
