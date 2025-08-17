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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class ServiceConfigurationBaseDto {

  @Valid
  @NotNull
  protected final Set<ServiceTemplateDto> templates;
  @Valid
  @NotNull
  protected final Set<ServiceDeploymentDto> deployments;
  @Valid
  @NotNull
  protected final Set<ServiceRemoteInclusionDto> includes;

  @NotNull
  protected final Document properties;

  protected ServiceConfigurationBaseDto(
    Set<ServiceTemplateDto> templates,
    Set<ServiceDeploymentDto> deployments,
    Set<ServiceRemoteInclusionDto> includes,
    Document properties
  ) {
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
    this.properties = properties;
  }
}
