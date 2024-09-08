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

import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.CorsConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpProxyMode;
import eu.cloudnetservice.ext.rest.api.config.SslConfiguration;
import eu.cloudnetservice.ext.rest.api.connection.EmptyConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.connection.HttpConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record RestConfiguration(
  int maxContentLength,
  boolean disableNativeTransport,
  @NonNull CorsConfig cors,
  @NonNull HttpProxyMode proxyMode,
  @NonNull List<HostAndPort> httpListeners,
  @NonNull List<ConnectionInfoResolverConfiguration> connectionInfoResolver,
  @Nullable SslConfiguration sslConfiguration
) {

  public static final RestConfiguration DEFAULT = new RestConfiguration(
    ComponentConfig.DEFAULT_MAX_CONTENT_LENGTH,
    false,
    CorsConfig.builder()
      .addAllowedOrigin("*")
      .addAllowedHeader("*")
      .allowCredentials(true)
      .build(),
    HttpProxyMode.DISABLED,
    List.of(new HostAndPort("127.0.0.1", 2812)),
    List.of(),
    null);

  public @NonNull ComponentConfig toComponentConfig() {
    return ComponentConfig.builder()
      .corsConfig(this.cors)
      .haProxyMode(this.proxyMode)
      .maxContentLength(this.maxContentLength)
      .sslConfiguration(this.sslConfiguration)
      .disableNativeTransport(this.disableNativeTransport)
      .connectionInfoResolver(this.httpConnectionInfoResolver())
      .executorService(Executors.newVirtualThreadPerTaskExecutor())
      .build();
  }

  private @NonNull HttpConnectionInfoResolver httpConnectionInfoResolver() {
    var baseResolver = EmptyConnectionInfoResolver.INSTANCE;
    for (var resolverConfiguration : this.connectionInfoResolver) {
      var resolver = resolverConfiguration.toResolver();
      if (resolver != null) {
        baseResolver = baseResolver.then(resolver);
      }
    }

    return baseResolver;
  }
}
