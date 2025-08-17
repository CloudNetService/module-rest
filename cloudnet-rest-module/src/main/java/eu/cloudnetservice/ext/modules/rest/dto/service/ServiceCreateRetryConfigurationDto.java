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

import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceCreateRetryConfiguration;
import eu.cloudnetservice.ext.modules.rest.dto.ChannelMessageTargetDto;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

public final class ServiceCreateRetryConfigurationDto implements Dto<ServiceCreateRetryConfiguration> {

  @Positive
  private final int maxRetries;
  @NotNull
  private final List<Long> backoffStrategy;
  @Valid
  @NotNull
  private final Map<ServiceCreateResult.State, List<ChannelMessageTargetDto>> eventReceivers;

  public ServiceCreateRetryConfigurationDto(
    int maxRetries,
    List<Long> backoffStrategy,
    Map<ServiceCreateResult.State, List<ChannelMessageTargetDto>> eventReceivers
  ) {
    this.maxRetries = maxRetries;
    this.backoffStrategy = backoffStrategy;
    this.eventReceivers = eventReceivers;
  }

  @Override
  public @NonNull ServiceCreateRetryConfiguration toEntity() {
    return ServiceCreateRetryConfiguration.builder()
      .maxRetries(this.maxRetries)
      .backoffStrategy(this.backoffStrategy.stream().map(Duration::ofMillis).toList())
      .notificationReceivers(this.eventReceivers.entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), Dto.toList(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
      .build();
  }
}
