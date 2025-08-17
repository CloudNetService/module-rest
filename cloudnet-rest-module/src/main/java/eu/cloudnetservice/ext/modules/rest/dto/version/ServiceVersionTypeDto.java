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

package eu.cloudnetservice.ext.modules.rest.dto.version;

import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import eu.cloudnetservice.node.impl.version.ServiceVersionType;
import eu.cloudnetservice.node.impl.version.execute.InstallStep;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;

public final class ServiceVersionTypeDto extends ServiceEnvironmentDto implements Dto<ServiceVersionType> {

  @Valid
  @NotNull
  private final List<InstallStep> installSteps;

  @Valid
  @NotNull
  private final Collection<ServiceVersionDto> versions;

  public ServiceVersionTypeDto(
    String name,
    String environmentType,
    List<InstallStep> installSteps,
    Collection<ServiceVersionDto> versions
  ) {
    super(name, environmentType);
    this.installSteps = installSteps;
    this.versions = versions;
  }

  @Override
  public @NonNull ServiceVersionType toEntity() {
    return new ServiceVersionType(this.name, this.environmentType, this.installSteps, Dto.toList(this.versions));
  }
}
