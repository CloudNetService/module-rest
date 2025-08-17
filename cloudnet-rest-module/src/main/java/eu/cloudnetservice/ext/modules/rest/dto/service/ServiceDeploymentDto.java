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
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.regex.Pattern;
import lombok.NonNull;

public final class ServiceDeploymentDto implements Dto<ServiceDeployment>  {

  @Valid
  @NotNull
  private final ServiceTemplateDto template;
  @NotNull
  private final Collection<Pattern> excludes;
  @NotNull
  private final Collection<Pattern> includes;
  @NotNull
  private final Document properties;

  public ServiceDeploymentDto(
    ServiceTemplateDto template,
    Collection<Pattern> excludes,
    Collection<Pattern> includes,
    Document properties
  ) {
    this.template = template;
    this.excludes = excludes;
    this.includes = includes;
    this.properties = properties;
  }

  @Override
  public @NonNull ServiceDeployment toEntity() {
    return ServiceDeployment.builder()
      .template(this.template.toEntity())
      .excludes(this.excludes)
      .includes(this.includes)
      .properties(this.properties)
      .build();
  }
}
