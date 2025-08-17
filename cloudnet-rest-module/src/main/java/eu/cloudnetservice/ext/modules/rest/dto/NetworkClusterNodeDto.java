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

import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.document.Document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.NonNull;

public final class NetworkClusterNodeDto implements Dto<NetworkClusterNode> {

  @NotBlank
  private final String uniqueId;
  @Valid
  @NotNull
  private final List<HostAndPortDto> listeners;
  @NotNull
  private final Document properties;

  public NetworkClusterNodeDto(String uniqueId, List<HostAndPortDto> listeners, Document properties) {
    this.uniqueId = uniqueId;
    this.listeners = listeners;
    this.properties = properties;
  }

  @Override
  public @NonNull NetworkClusterNode toEntity() {
    return new NetworkClusterNode(this.uniqueId, Dto.toList(this.listeners), this.properties);
  }
}
