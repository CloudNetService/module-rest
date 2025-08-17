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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public class CloudOfflinePlayerDto implements Dto<CloudOfflinePlayer> {

  @NotBlank
  private final String name;

  @Min(-1)
  private final long firstLoginTimeMillis;
  @Min(-1)
  private final long lastLoginTimeMillis;

  @Valid
  @NotNull
  private final NetworkPlayerProxyInfoDto lastNetworkPlayerProxyInfo;

  @NotNull
  private final Document properties;

  public CloudOfflinePlayerDto(
    String name,
    long firstLoginTimeMillis,
    long lastLoginTimeMillis,
    NetworkPlayerProxyInfoDto lastNetworkPlayerProxyInfo,
    Document properties
  ) {
    this.name = name;
    this.firstLoginTimeMillis = firstLoginTimeMillis;
    this.lastLoginTimeMillis = lastLoginTimeMillis;
    this.lastNetworkPlayerProxyInfo = lastNetworkPlayerProxyInfo;
    this.properties = properties;
  }

  @Override
  public @NonNull CloudOfflinePlayer toEntity() {
    return new CloudOfflinePlayer(
      this.name,
      this.firstLoginTimeMillis,
      this.lastLoginTimeMillis,
      this.lastNetworkPlayerProxyInfo.toEntity(),
      this.properties.immutableCopy());
  }

}
