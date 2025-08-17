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

package eu.cloudnetservice.ext.modules.rest.dto;

import eu.cloudnetservice.driver.cluster.NetworkCluster;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;

public final class NetworkClusterDto implements Dto<NetworkCluster> {

  @NotNull
  private final UUID clusterId;
  @Valid
  @NotNull
  private final Collection<NetworkClusterNodeDto> nodes;

  public NetworkClusterDto(UUID clusterId, Collection<NetworkClusterNodeDto> nodes) {
    this.clusterId = clusterId;
    this.nodes = nodes;
  }

  @Override
  public @NonNull NetworkCluster toEntity() {
    return new NetworkCluster(this.clusterId, Dto.toList(this.nodes));
  }
}
