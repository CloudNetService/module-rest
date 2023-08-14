/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.rest.http.config;

import eu.cloudnetservice.ext.rest.http.connection.EmptyConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.http.connection.HttpConnectionInfoResolver;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record ComponentConfig(
  boolean disableNativeTransport,
  @NonNull CorsConfig corsConfig,
  @NonNull HttpProxyMode haProxyMode,
  @Nullable SslConfiguration sslConfiguration,
  @NonNull HttpConnectionInfoResolver connectionInfoResolver
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ComponentConfig contextConfig) {
    return new Builder().corsConfig(contextConfig.corsConfig());
  }

  public static final class Builder {

    private boolean disableNativeTransport;
    private SslConfiguration sslConfiguration;
    private HttpProxyMode haProxyMode = HttpProxyMode.DISABLED;
    private CorsConfig.Builder corsConfigBuilder = CorsConfig.builder();
    private HttpConnectionInfoResolver connectionInfoResolver = EmptyConnectionInfoResolver.INSTANCE;

    public @NonNull Builder disableNativeTransport() {
      this.disableNativeTransport = true;
      return this;
    }

    public @NonNull Builder sslConfiguration(@Nullable SslConfiguration sslConfiguration) {
      this.sslConfiguration = sslConfiguration;
      return this;
    }

    public @NonNull Builder haProxyMode(@NonNull HttpProxyMode haProxyMode) {
      this.haProxyMode = haProxyMode;
      return this;
    }

    public @NonNull Builder corsConfig(@NonNull CorsConfig corsConfig) {
      this.corsConfigBuilder = CorsConfig.builder(corsConfig);
      return this;
    }

    public @NonNull Builder corsConfig(@NonNull CorsConfig.Builder corsConfigBuilder) {
      this.corsConfigBuilder = corsConfigBuilder;
      return this;
    }

    public @NonNull Builder connectionInfoResolver(@NonNull HttpConnectionInfoResolver resolver) {
      this.connectionInfoResolver = resolver;
      return this;
    }

    public @NonNull Builder appendConnectionInfoResolveStep(@NonNull HttpConnectionInfoResolver resolveStep) {
      this.connectionInfoResolver = this.connectionInfoResolver.then(resolveStep);
      return this;
    }

    public @NonNull ComponentConfig build() {
      return new ComponentConfig(
        this.disableNativeTransport,
        this.corsConfigBuilder.build(),
        this.haProxyMode,
        this.sslConfiguration,
        this.connectionInfoResolver);
    }
  }
}
