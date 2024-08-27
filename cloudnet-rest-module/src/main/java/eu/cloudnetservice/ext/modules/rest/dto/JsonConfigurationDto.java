/*
 * Copyright 2019-2024 CloudNetService team & contributors
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
import eu.cloudnetservice.node.config.JsonConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

public final class JsonConfigurationDto implements Dto<JsonConfiguration> {

  @NotBlank
  private final String language;

  @Valid
  @NotNull
  private final NetworkClusterNodeDto identity;
  @Valid
  @NotNull
  private final NetworkClusterDto clusterConfig;

  @NotNull
  private final Set<String> ipWhitelist;

  @Positive
  private final double maxCPUUsageToStartServices;

  @Positive
  private final int maxMemory;
  @Positive
  private final int maxServiceConsoleLogCacheSize;
  @Positive
  private final int processTerminationTimeoutSeconds;

  @NotNull
  private final boolean forceInitialClusterDataSync;
  @NotNull
  private final boolean printErrorStreamLinesFromServices;
  @NotNull
  private final boolean runBlockedServiceStartTryLaterAutomatic;

  private final String jvmCommand;
  @NotNull
  private final String hostAddress;
  @NotNull
  private final Map<String, String> ipAliases;

  @Valid
  @NotNull
  private final SSLConfigurationDto clientSslConfig;
  @Valid
  @NotNull
  private final SSLConfigurationDto serverSslConfig;

  @NotNull
  private final Document properties;

  public JsonConfigurationDto(
    String language,
    NetworkClusterNodeDto identity,
    NetworkClusterDto clusterConfig,
    Set<String> ipWhitelist,
    double maxCPUUsageToStartServices,
    int maxMemory,
    int maxServiceConsoleLogCacheSize,
    int processTerminationTimeoutSeconds,
    boolean forceInitialClusterDataSync,
    boolean printErrorStreamLinesFromServices,
    boolean runBlockedServiceStartTryLaterAutomatic,
    String jvmCommand,
    String hostAddress,
    Map<String, String> ipAliases,
    SSLConfigurationDto clientSslConfig,
    SSLConfigurationDto serverSslConfig,
    Document properties
  ) {
    this.language = language;
    this.identity = identity;
    this.clusterConfig = clusterConfig;
    this.ipWhitelist = ipWhitelist;
    this.maxCPUUsageToStartServices = maxCPUUsageToStartServices;
    this.maxMemory = maxMemory;
    this.maxServiceConsoleLogCacheSize = maxServiceConsoleLogCacheSize;
    this.processTerminationTimeoutSeconds = processTerminationTimeoutSeconds;
    this.forceInitialClusterDataSync = forceInitialClusterDataSync;
    this.printErrorStreamLinesFromServices = printErrorStreamLinesFromServices;
    this.runBlockedServiceStartTryLaterAutomatic = runBlockedServiceStartTryLaterAutomatic;
    this.jvmCommand = jvmCommand;
    this.hostAddress = hostAddress;
    this.ipAliases = ipAliases;
    this.clientSslConfig = clientSslConfig;
    this.serverSslConfig = serverSslConfig;
    this.properties = properties;
  }

  @Override
  public @NonNull JsonConfiguration toEntity() {
    var config = new JsonConfiguration();
    config.language(this.language);
    config.identity(this.identity.toEntity());
    config.clusterConfig(this.clusterConfig.toEntity());
    config.ipWhitelist(this.ipWhitelist);
    config.maxCPUUsageToStartServices(this.maxCPUUsageToStartServices);
    config.maxMemory(this.maxMemory);
    config.maxServiceConsoleLogCacheSize(this.maxServiceConsoleLogCacheSize);
    config.processTerminationTimeoutSeconds(this.processTerminationTimeoutSeconds);
    config.forceInitialClusterDataSync(this.forceInitialClusterDataSync);
    config.printErrorStreamLinesFromServices(this.printErrorStreamLinesFromServices);
    config.runBlockedServiceStartTryLaterAutomatic(this.runBlockedServiceStartTryLaterAutomatic);
    config.javaCommand(this.jvmCommand);
    config.hostAddress(this.hostAddress);
    config.ipAliases(this.ipAliases);
    config.clientSSLConfig(this.clientSslConfig.toEntity());
    config.serverSSLConfig(this.serverSslConfig.toEntity());
    config.properties(this.properties);
    return config;
  }
}
