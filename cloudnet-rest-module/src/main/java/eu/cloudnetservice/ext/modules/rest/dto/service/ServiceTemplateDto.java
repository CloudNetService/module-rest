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

import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

public final class ServiceTemplateDto implements Dto<ServiceTemplate> {

  @NotBlank
  private final String prefix;
  @NotBlank
  private final String name;
  @NotBlank
  private final String storage;
  private final int priority;
  private final boolean alwaysCopyToStaticServices;

  public ServiceTemplateDto(
    String prefix,
    String name,
    String storage,
    int priority,
    boolean alwaysCopyToStaticServices
  ) {
    this.prefix = prefix;
    this.name = name;
    this.storage = storage;
    this.priority = priority;
    this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
  }

  @Override
  public @NonNull ServiceTemplate toEntity() {
    return ServiceTemplate.builder()
      .prefix(this.prefix)
      .name(this.name)
      .storage(this.storage)
      .priority(this.priority)
      .alwaysCopyToStaticServices(this.alwaysCopyToStaticServices)
      .build();
  }
}
