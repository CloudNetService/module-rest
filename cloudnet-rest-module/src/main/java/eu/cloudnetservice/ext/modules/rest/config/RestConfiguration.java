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

package eu.cloudnetservice.ext.modules.rest.config;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.CorsConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpProxyMode;
import eu.cloudnetservice.ext.rest.api.config.SslConfiguration;
import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RestConfiguration(
  boolean disableNativeTransport,
  @NonNull CorsConfig corsConfig,
  @NonNull HttpProxyMode proxyMode,
  @NonNull AuthConfiguration authConfig,
  @NonNull List<HostAndPort> httpListeners,
  @Nullable SslConfiguration sslConfiguration
) {

  public static final RestConfiguration DEFAULT = new RestConfiguration(
    false,
    CorsConfig.builder()
      .addAllowedOrigin("*")
      .addAllowedHeader("*")
      .allowCredentials(true)
      .build(),
    HttpProxyMode.DISABLED,
    AuthConfiguration.DEFAULT_CONFIGURATION,
    List.of(new HostAndPort("127.0.0.1", 2812)),
    null);

  private static RestConfiguration instance;

  public static @NotNull RestConfiguration get() {
    Preconditions.checkState(instance != null, "rest configuration has not been initialized");
    return instance;
  }

  public static void setInstance(@NotNull RestConfiguration config) {
    instance = config;
  }

  public void validate() {
    this.authConfig.validate();
  }

  public @NonNull ComponentConfig toComponentConfig() {
    return ComponentConfig.builder()
      .corsConfig(this.corsConfig)
      .haProxyMode(this.proxyMode)
      .sslConfiguration(this.sslConfiguration)
      .disableNativeTransport(this.disableNativeTransport)
      .executorService(Executors.newVirtualThreadPerTaskExecutor())
      .build();
  }
}
