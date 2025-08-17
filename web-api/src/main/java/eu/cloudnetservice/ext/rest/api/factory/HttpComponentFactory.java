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

package eu.cloudnetservice.ext.rest.api.factory;

import eu.cloudnetservice.ext.rest.api.HttpComponent;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import lombok.NonNull;

/**
 * A http component factory allowing to construct new http components based on the implementation loaded in the runtime
 * without needing to access the implementation at compile time.
 * <p>
 * Use the {@link HttpComponentFactoryLoader} to load the component factory needed to construct the desired component.
 *
 * @param <T> the component this factory constructs.
 * @see HttpComponentFactoryLoader
 * @since 1.0
 */
public interface HttpComponentFactory<T extends HttpComponent<T>> {

  /**
   * The name of the constructed component.
   *
   * @return name of the constructed component.
   */
  @NonNull String componentTypeName();

  /**
   * Gets the supported component type for this component factory.
   *
   * @return the supported component type for this component factory.
   */
  @NonNull Class<T> supportedComponentType();

  /**
   * Constructs a new instance of the component this factory targets.
   *
   * @param componentConfig the component config for the component.
   * @return the newly constructed component.
   * @throws NullPointerException if the given component config is null.
   */
  @NonNull T construct(@NonNull ComponentConfig componentConfig);
}
