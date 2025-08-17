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

package eu.cloudnetservice.ext.modules.rest.dto.bridge.player;

import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import eu.cloudnetservice.ext.modules.rest.dto.HostAndPortDto;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.NonNull;

public class NetworkPlayerProxyInfoDto implements Dto<NetworkPlayerProxyInfo> {

  @NotNull
  private final UUID uniqueId;
  @NotBlank
  private final String name;

  private final String xBoxId;
  @Min(47)
  private final int version;

  @Valid
  @NotNull
  private final HostAndPortDto address;
  @Valid
  @NotNull
  private final HostAndPortDto listener;
  private final boolean onlineMode;

  @Valid
  @NotNull
  private final NetworkServiceInfoDto networkService;

  public NetworkPlayerProxyInfoDto(
    UUID uniqueId,
    String name,
    String xBoxId,
    int version,
    HostAndPortDto address,
    HostAndPortDto listener,
    boolean onlineMode,
    NetworkServiceInfoDto networkService
  ) {
    this.uniqueId = uniqueId;
    this.name = name;
    this.xBoxId = xBoxId;
    this.version = version;
    this.address = address;
    this.listener = listener;
    this.onlineMode = onlineMode;
    this.networkService = networkService;
  }


  @Override
  public @NonNull NetworkPlayerProxyInfo toEntity() {
    return new NetworkPlayerProxyInfo(
      this.uniqueId,
      this.name,
      this.xBoxId,
      this.version,
      this.address.toEntity(),
      this.listener.toEntity(),
      this.onlineMode,
      this.networkService.toEntity());
  }
}
