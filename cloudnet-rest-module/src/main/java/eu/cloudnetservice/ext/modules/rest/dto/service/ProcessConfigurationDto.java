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

import eu.cloudnetservice.driver.service.ProcessConfiguration;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public final class ProcessConfigurationDto implements Dto<ProcessConfiguration.Builder> {

  @NotBlank
  private final String environment;
  @Positive
  private final int maxHeapMemorySize;
  @NotNull
  private final List<String> jvmOptions;
  @NotNull
  private final List<String> processParameters;
  @NotNull
  private final Map<String, String> environmentVariables;

  public ProcessConfigurationDto(
    String environment,
    int maxHeapMemorySize,
    List<String> jvmOptions,
    List<String> processParameters,
    Map<String, String> environmentVariables
  ) {
    this.environment = environment;
    this.maxHeapMemorySize = maxHeapMemorySize;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.environmentVariables = environmentVariables;
  }

  @Override
  public @NonNull ProcessConfiguration.Builder toEntity() {
    return ProcessConfiguration.builder()
      .environment(this.environment)
      .maxHeapMemorySize(this.maxHeapMemorySize)
      .jvmOptions(this.jvmOptions)
      .processParameters(this.processParameters)
      .environmentVariables(this.environmentVariables);
  }
}
