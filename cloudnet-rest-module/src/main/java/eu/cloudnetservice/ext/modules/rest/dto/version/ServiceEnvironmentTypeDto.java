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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.NonNull;

public final class ServiceEnvironmentTypeDto implements Dto<ServiceEnvironmentType> {

  @NotBlank
  private final String name;
  private final int defaultServiceStartPort;
  @NotNull
  private final Set<String> defaultProcessArguments;
  @NotNull
  private final Document properties;

  public ServiceEnvironmentTypeDto(
    String name,
    int defaultServiceStartPort,
    Set<String> defaultProcessArguments,
    Document properties
  ) {
    this.name = name;
    this.defaultServiceStartPort = defaultServiceStartPort;
    this.defaultProcessArguments = defaultProcessArguments;
    this.properties = properties;
  }

  @Override
  public @NonNull ServiceEnvironmentType toEntity() {
    return ServiceEnvironmentType.builder()
      .name(this.name)
      .defaultServiceStartPort(this.defaultServiceStartPort)
      .defaultProcessArguments(this.defaultProcessArguments)
      .properties(this.properties)
      .build();
  }
}
