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

package eu.cloudnetservice.ext.rest.api.config;

import static eu.cloudnetservice.ext.rest.api.config.HttpProxyMode.DISABLED;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.connection.EmptyConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.connection.HttpConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.PlainTextResponse;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record ComponentConfig(
  int maxContentLength,
  boolean disableNativeTransport,
  @NonNull CorsConfig corsConfig,
  @NonNull HttpProxyMode haProxyMode,
  @NonNull HttpHandler fallbackHttpHandler,
  @NonNull ExecutorService executorService,
  @Nullable SslConfiguration sslConfiguration,
  @NonNull HttpConnectionInfoResolver connectionInfoResolver
) {

  public static final int DEFAULT_MAX_CONTENT_LENGTH = 5 * 1024 * 1024;

  private static final HttpHandler DEFAULT_FALLBACK_HANDLER = new HttpHandler() {
    @Override
    public @NonNull IntoResponse<?> handle(@NonNull HttpContext context) {
      return PlainTextResponse.builder().notFound();
    }
  };

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ComponentConfig componentConfig) {
    return new Builder()
      .disableNativeTransport(componentConfig.disableNativeTransport())
      .corsConfig(componentConfig.corsConfig())
      .haProxyMode(componentConfig.haProxyMode())
      .fallbackHttpHandler(componentConfig.fallbackHttpHandler())
      .sslConfiguration(componentConfig.sslConfiguration())
      .connectionInfoResolver(componentConfig.connectionInfoResolver());
  }

  public static final class Builder {

    private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
    private boolean disableNativeTransport;
    private HttpHandler fallbackHttpHandler = DEFAULT_FALLBACK_HANDLER;
    private ExecutorService executorService;
    private SslConfiguration sslConfiguration;
    private HttpProxyMode haProxyMode = DISABLED;
    private CorsConfig.Builder corsConfigBuilder = CorsConfig.builder();
    private HttpConnectionInfoResolver connectionInfoResolver = EmptyConnectionInfoResolver.INSTANCE;

    public @NonNull Builder maxContentLength(int maxContentLength) {
      Preconditions.checkArgument(maxContentLength > 0, "maxContentLength must be greater than 0");

      this.maxContentLength = maxContentLength;
      return this;
    }

    public @NonNull Builder disableNativeTransport(boolean disableNativeTransport) {
      this.disableNativeTransport = disableNativeTransport;
      return this;
    }

    public @NonNull Builder fallbackHttpHandler(@NonNull HttpHandler fallbackHttpHandler) {
      this.fallbackHttpHandler = fallbackHttpHandler;
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

    public @NonNull Builder executorService(@NonNull ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public @NonNull ComponentConfig build() {
      Preconditions.checkNotNull(this.executorService, "Missing executor service");

      return new ComponentConfig(
        this.maxContentLength,
        this.disableNativeTransport,
        this.corsConfigBuilder.build(),
        this.haProxyMode,
        this.fallbackHttpHandler,
        this.executorService,
        this.sslConfiguration,
        this.connectionInfoResolver);
    }
  }
}
