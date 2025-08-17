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

package eu.cloudnetservice.ext.rest.api.annotation.parser.processor;

import eu.cloudnetservice.ext.rest.api.annotation.CrossOrigin;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.config.CorsConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import lombok.NonNull;

/**
 * A processor for the {@code @CrossOrigin} annotation.
 *
 * @since 1.0
 */
public final class CrossOriginProcessor implements HttpAnnotationProcessor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handlerInstance
  ) {
    var annotation = method.getAnnotation(CrossOrigin.class);
    if (annotation != null) {
      var corsConfig = CorsConfig.builder();
      for (var origin : annotation.origins()) {
        corsConfig.addAllowedOrigin(origin);
      }

      corsConfig.allowedHeaders(List.of(annotation.allowedHeaders()))
        .exposedHeaders(List.of(annotation.exposedHeaders()))
        .allowCredentials(annotation.allowCredentials().toBoolean())
        .allowPrivateNetworks(annotation.allowPrivateNetworks().toBoolean())
        .maxAge(annotation.maxAge() < 1 ? null : Duration.ofSeconds(annotation.maxAge()));

      // apply the extracted cors configuration
      config.corsConfiguration(corsConfig.build());
    }
  }
}
