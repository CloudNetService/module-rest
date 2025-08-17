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

package eu.cloudnetservice.ext.rest.api.config;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.response.Response;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record HttpHandlerConfig(
  @NonNull HttpMethod httpMethod,
  @Nullable CorsConfig corsConfig,
  @NonNull List<HttpHandlerInterceptor> handlerInterceptors
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull HttpHandlerConfig config) {
    return builder()
      .httpMethod(config.httpMethod())
      .corsConfiguration(config.corsConfig())
      .handlerInterceptors(config.handlerInterceptors());
  }

  public boolean invokePreProcessors(
    @NonNull HttpContext context,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config
  ) throws Exception {
    for (var interceptor : this.handlerInterceptors) {
      if (!interceptor.preProcess(context, handler, config)) {
        return false;
      }
    }
    return true;
  }

  public boolean invokePostProcessors(
    @NonNull HttpContext context,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config,
    @NonNull Response<?> response
  ) throws Exception {
    for (var interceptor : this.handlerInterceptors) {
      if (!interceptor.postProcess(context, handler, config, response)) {
        return false;
      }
    }
    return true;
  }

  public void invokeExceptionallyPostProcessors(
    @NonNull HttpContext context,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config,
    @NonNull Throwable exception
  ) throws Exception {
    for (var interceptor : this.handlerInterceptors) {
      interceptor.postProcessExceptionally(context, handler, config, exception);
    }
  }

  public static final class Builder {

    private HttpMethod httpMethod;
    private CorsConfig corsConfig;
    private List<HttpHandlerInterceptor> handlerInterceptors = new LinkedList<>();

    private Builder() {
    }

    public @NonNull Builder httpMethod(@NonNull HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public @NonNull Builder corsConfiguration(@Nullable CorsConfig corsConfig) {
      this.corsConfig = corsConfig;
      return this;
    }

    public @NonNull Builder addHandlerInterceptor(@NonNull HttpHandlerInterceptor interceptor) {
      this.handlerInterceptors.add(interceptor);
      return this;
    }

    public @NonNull Builder handlerInterceptors(@NonNull List<HttpHandlerInterceptor> handlerInterceptors) {
      this.handlerInterceptors = new LinkedList<>(handlerInterceptors);
      return this;
    }

    public @NonNull Builder modifyHandlerInterceptors(
      @NonNull Consumer<List<HttpHandlerInterceptor>> interceptorModifier) {
      interceptorModifier.accept(this.handlerInterceptors);
      return this;
    }

    public @NonNull HttpHandlerConfig build() {
      Preconditions.checkNotNull(this.httpMethod, "http method is required");

      return new HttpHandlerConfig(
        this.httpMethod,
        this.corsConfig,
        Arrays.asList(this.handlerInterceptors.toArray(HttpHandlerInterceptor[]::new)));
    }
  }
}
