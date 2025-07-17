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

import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public final class ChannelMessageTargetDto implements Dto<ChannelMessageTarget> {

  @NotNull
  private final ChannelMessageTarget.Type type;
  private final String name;

  public ChannelMessageTargetDto(ChannelMessageTarget.Type type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public @NonNull ChannelMessageTarget toEntity() {
    return switch (this.type) {
      case ALL -> ChannelMessageTarget.all();
      case NODE -> this.name == null ? ChannelMessageTarget.allNodes() : ChannelMessageTarget.node(this.name);
      case SERVICE -> this.name == null ? ChannelMessageTarget.allServices() : ChannelMessageTarget.service(this.name);
      case SERVICES_BY_TASK -> ChannelMessageTarget.servicesByTask(this.name);
      case SERVICES_BY_GROUP -> ChannelMessageTarget.servicesByGroup(this.name);
      case SERVICES_BY_ENV -> ChannelMessageTarget.servicesByEnvironment(this.name);
      case SERVICES_WITH_PROPERTY -> ChannelMessageTarget.servicesWithProperty(this.name);
    };
  }
}
