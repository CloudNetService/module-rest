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

package eu.cloudnetservice.ext.modules.rest.dto.bridge.player;

import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceIdDto;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.NonNull;

public class NetworkServiceInfoDto implements Dto<NetworkServiceInfo> {

  @NotNull
  private final Set<String> groups;
  @Valid
  @NotNull
  private final ServiceIdDto serviceId;

  public NetworkServiceInfoDto(Set<String> groups, ServiceIdDto serviceId) {
    this.groups = groups;
    this.serviceId = serviceId;
  }

  @Override
  public @NonNull NetworkServiceInfo toEntity() {
    return new NetworkServiceInfo(this.groups, this.serviceId.toEntity().build());
  }
}
