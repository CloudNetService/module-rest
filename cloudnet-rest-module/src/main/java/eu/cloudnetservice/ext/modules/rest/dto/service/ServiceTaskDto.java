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

package eu.cloudnetservice.ext.modules.rest.dto.service;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;
import lombok.NonNull;

public final class ServiceTaskDto extends ServiceConfigurationBaseDto implements Dto<ServiceTask> {

  @NotNull
  @Pattern(regexp = ServiceTask.NAMING_REGEX)
  private final String name;

  @NotBlank
  private final String runtime;
  private final String hostAddress;
  private final String javaCommand;

  @NotNull
  @Pattern(regexp = ServiceTask.NAMING_REGEX)
  private final String nameSplitter;

  private final boolean disableIpRewrite;
  private final boolean maintenance;
  private final boolean autoDeleteOnStop;
  private final boolean staticServices;

  @NotNull
  private final Set<String> groups;
  @NotNull
  private final Set<String> associatedNodes;
  @NotNull
  private final Set<String> deletedFilesAfterStop;

  @Valid
  @NotNull
  private final ProcessConfigurationDto processConfiguration;

  @Min(1)
  @Max(0xFFFF)
  private final int startPort;
  @PositiveOrZero
  private final int minServiceCount;

  public ServiceTaskDto(
    String name,
    String runtime,
    String hostAddress,
    String javaCommand,
    String nameSplitter,
    boolean disableIpRewrite,
    boolean maintenance,
    boolean autoDeleteOnStop,
    boolean staticServices,
    Set<String> groups,
    Set<String> associatedNodes,
    Set<String> deletedFilesAfterStop,
    ProcessConfigurationDto processConfiguration,
    int startPort,
    int minServiceCount,
    Set<ServiceTemplateDto> templates,
    Set<ServiceDeploymentDto> deployments,
    Set<ServiceRemoteInclusionDto> includes,
    Document properties
  ) {
    super(templates, deployments, includes, properties);
    this.name = name;
    this.runtime = runtime;
    this.hostAddress = hostAddress;
    this.javaCommand = javaCommand;
    this.nameSplitter = nameSplitter;
    this.disableIpRewrite = disableIpRewrite;
    this.maintenance = maintenance;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.staticServices = staticServices;
    this.associatedNodes = associatedNodes;
    this.groups = groups;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
    this.processConfiguration = processConfiguration;
    this.startPort = startPort;
    this.minServiceCount = minServiceCount;
  }

  @Override
  public @NonNull ServiceTask toEntity() {
    return ServiceTask.builder()
      .name(this.name)
      .javaCommand(this.javaCommand)
      .runtime(this.runtime)
      .hostAddress(this.hostAddress)
      .nameSplitter(this.nameSplitter)

      .maintenance(this.maintenance)
      .staticServices(this.staticServices)
      .disableIpRewrite(this.disableIpRewrite)
      .autoDeleteOnStop(this.autoDeleteOnStop)

      .groups(this.groups)
      .associatedNodes(this.associatedNodes)
      .deletedFilesAfterStop(this.deletedFilesAfterStop)

      .templates(Dto.toList(this.templates))
      .deployments(Dto.toList(this.deployments))
      .inclusions(Dto.toList(this.includes))

      .startPort(this.startPort)
      .minServiceCount(this.minServiceCount)

      .properties(this.properties)
      .processConfiguration(this.processConfiguration.toEntity())
      .build();
  }
}
