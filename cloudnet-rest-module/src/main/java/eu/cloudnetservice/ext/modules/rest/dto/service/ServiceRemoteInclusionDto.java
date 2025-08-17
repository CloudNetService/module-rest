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
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public final class ServiceRemoteInclusionDto implements Dto<ServiceRemoteInclusion> {

  @NotBlank
  private final String url;
  @NotBlank
  private final String destination;
  @NotNull
  private final Document properties;

  public ServiceRemoteInclusionDto(
    String url,
    String destination,
    Document properties
  ) {
    this.url = url;
    this.destination = destination;
    this.properties = properties;
  }

  @Override
  public @NonNull ServiceRemoteInclusion toEntity() {
    return ServiceRemoteInclusion.builder()
      .url(this.url)
      .destination(this.destination)
      .properties(this.properties)
      .build();
  }
}
