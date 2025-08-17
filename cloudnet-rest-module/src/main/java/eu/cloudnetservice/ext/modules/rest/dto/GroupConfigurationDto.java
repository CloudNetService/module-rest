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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceConfigurationBaseDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceDeploymentDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceRemoteInclusionDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceTemplateDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

public final class GroupConfigurationDto extends ServiceConfigurationBaseDto implements Dto<GroupConfiguration> {

  @NotBlank
  private final String name;

  @NotNull
  private final Set<String> jvmOptions;
  @NotNull
  private final Set<String> processParameters;
  @NotNull
  private final Map<String, String> environmentVariables;

  @NotNull
  private final Set<String> targetEnvironments;

  public GroupConfigurationDto(
    String name,
    Set<String> jvmOptions,
    Set<String> processParameters,
    Map<String, String> environmentVariables,
    Set<String> targetEnvironments,
    Set<ServiceTemplateDto> templates,
    Set<ServiceDeploymentDto> deployments,
    Set<ServiceRemoteInclusionDto> includes,
    Document properties
  ) {
    super(templates, deployments, includes, properties);

    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
    this.environmentVariables = environmentVariables;
  }

  @Override
  public @NonNull GroupConfiguration toEntity() {
    return GroupConfiguration.builder()
      .name(this.name)
      .jvmOptions(this.jvmOptions)
      .processParameters(this.processParameters)
      .environmentVariables(this.environmentVariables)
      .targetEnvironments(this.targetEnvironments)
      .templates(Dto.toList(this.templates))
      .deployments(Dto.toList(this.deployments))
      .inclusions(Dto.toList(this.includes))
      .build();
  }
}
