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
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateRetryConfiguration;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.NonNull;

public final class ServiceConfigurationDto extends ServiceConfigurationBaseDto implements Dto<ServiceConfiguration> {

  @Valid
  @NotNull
  private final ServiceIdDto serviceId;
  @Valid
  @NotNull
  private final ProcessConfigurationDto processConfig;
  @Valid
  private final ServiceCreateRetryConfigurationDto retryConfiguration;

  @Min(1)
  @Max(0xFFFF)
  private final int port;
  @NotBlank
  private final String runtime;
  private final String hostAddress;
  private final String javaCommand;

  private final boolean autoDeleteOnStop;
  private final boolean staticService;

  @NotNull
  private final Set<String> groups;
  @NotNull
  private final Set<String> deletedFilesAfterStop;

  public ServiceConfigurationDto(
    ServiceIdDto serviceId,
    ProcessConfigurationDto processConfig,
    ServiceCreateRetryConfigurationDto retryConfiguration,
    int port,
    String runtime,
    String hostAddress,
    String javaCommand,
    boolean autoDeleteOnStop,
    boolean staticService,
    Set<String> groups,
    Set<String> deletedFilesAfterStop,
    Set<ServiceTemplateDto> templates,
    Set<ServiceDeploymentDto> deployments,
    Set<ServiceRemoteInclusionDto> includes,
    Document properties
  ) {
    super(templates, deployments, includes, properties);

    this.serviceId = serviceId;
    this.port = port;
    this.runtime = runtime;
    this.hostAddress = hostAddress;
    this.javaCommand = javaCommand;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.staticService = staticService;
    this.processConfig = processConfig;
    this.groups = groups;
    this.retryConfiguration = retryConfiguration;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
  }

  @Override
  public @NonNull ServiceConfiguration toEntity() {
    var retry = this.retryConfiguration == null
      ? ServiceCreateRetryConfiguration.NO_RETRY
      : this.retryConfiguration.toEntity();

    return ServiceConfiguration.builder()
      .serviceId(this.serviceId.toEntity())
      .runtime(this.runtime)
      .hostAddress(this.hostAddress)
      .javaCommand(this.javaCommand)
      .autoDeleteOnStop(this.autoDeleteOnStop)
      .staticService(this.staticService)
      .startPort(this.port)
      .processConfig(this.processConfig.toEntity())
      .groups(this.groups)
      .deletedFilesAfterStop(this.deletedFilesAfterStop)
      .templates(Dto.toList(this.templates))
      .deployments(Dto.toList(this.deployments))
      .inclusions(Dto.toList(this.includes))
      .properties(this.properties)
      .retryConfiguration(retry)
      .build();
  }
}
