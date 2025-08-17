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

package eu.cloudnetservice.ext.rest.api;

import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.registry.HttpHandlerRegistry;
import lombok.NonNull;

/**
 * Represents any http component, providing an abstract layer for registering listeners to it.
 *
 * @param <T> the generic type of the component implementing this class.
 * @see HttpServer
 * @since 1.0
 */
public interface HttpComponent<T extends HttpComponent<T>> extends AutoCloseable {

  /**
   * Gets whether this component has ssl enabled or not.
   *
   * @return whether this component has ssl enabled or not.
   */
  boolean sslEnabled();

  /**
   * Gets the configuration of this http component.
   *
   * @return the configuration of this component.
   */
  @NonNull ComponentConfig componentConfig();

  /**
   * Gets a http annotation parser which is associated with this component and can therefore be used to register
   * annotated handlers to this component.
   *
   * @return the associated http annotation parser instance.
   */
  @NonNull HttpAnnotationParser annotationParser();

  /**
   * Gets a http handler registry which is associated with this component and can therefore be used to register and
   * unregister http handlers.
   * <p>
   * Unlike with the {@link #annotationParser()} you can't register annotated handlers directly to this component.
   *
   * @return the associated http handler registry instance.
   */
  @NonNull HttpHandlerRegistry handlerRegistry();
}
