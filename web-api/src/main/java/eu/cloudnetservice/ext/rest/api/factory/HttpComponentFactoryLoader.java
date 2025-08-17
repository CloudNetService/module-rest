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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.ext.rest.api.HttpComponent;
import java.util.Collection;
import java.util.Objects;
import java.util.ServiceLoader;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A component factory loader that loads the implementation based component factories using the {@link ServiceLoader}
 * from Java SPI.
 *
 * @see HttpComponentFactory
 * @see ServiceLoader
 * @since 1.0
 */
public final class HttpComponentFactoryLoader {

  /**
   * Loads all component factories that are present in the runtime ignoring the target component.
   *
   * @return all component factories that are present in the runtime.
   */
  public static @NonNull Collection<? extends HttpComponentFactory<?>> loadComponentFactories() {
    var serviceLoader = ServiceLoader.load(HttpComponentFactory.class, HttpComponentFactory.class.getClassLoader());
    return serviceLoader.stream()
      .map(ServiceLoader.Provider::get)
      .map(factory -> (HttpComponentFactory<?>) factory)
      .toList();
  }

  /**
   * Loads all component factories that are present in the runtime that target the given component type.
   *
   * @param componentType the component type the factory has to target.
   * @param <T>           the type the factory constructs.
   * @return all component factories that are present in the runtime that target the given component type.
   * @throws NullPointerException if the given component type is null.
   */
  @SuppressWarnings("unchecked")
  public static @NonNull <T extends HttpComponent<T>> Collection<HttpComponentFactory<T>> loadComponentFactories(
    @NonNull Class<T> componentType
  ) {
    var serviceLoader = ServiceLoader.load(HttpComponentFactory.class, HttpComponentFactory.class.getClassLoader());
    return serviceLoader.stream()
      .map(ServiceLoader.Provider::get)
      .filter(factory -> factory.supportedComponentType() == componentType)
      .map(factory -> (HttpComponentFactory<T>) factory)
      .toList();
  }

  /**
   * Finds the first component factory that is present in the runtime that targets the given component type.
   *
   * @param componentType the component type the factory has to target.
   * @param <T>           the type the factory constructs.
   * @return the first component factory, null if no factory targets the given type.
   * @throws NullPointerException if the given component type is null.
   */
  public static @Nullable <T extends HttpComponent<T>> HttpComponentFactory<T> findFirstComponentFactory(
    @NonNull Class<T> componentType
  ) {
    var componentFactories = loadComponentFactories(componentType);
    return Iterables.getFirst(componentFactories, null);
  }

  /**
   * Gets the first component factory that is present in the runtime that targets the given component type.
   * <p>
   * If no factory is found this method will throw {@link NullPointerException} instead of returning null.
   *
   * @param componentType the component type the factory has to target.
   * @param <T>           the type the factory constructs.
   * @return the first component factory.
   * @throws NullPointerException if the given component type is null or no factory was found.
   */
  public static @NonNull <T extends HttpComponent<T>> HttpComponentFactory<T> getFirstComponentFactory(
    @NonNull Class<T> componentType
  ) {
    return Objects.requireNonNull(
      findFirstComponentFactory(componentType),
      "No component factory for " + componentType);
  }

  private HttpComponentFactoryLoader() {
    throw new UnsupportedOperationException();
  }
}
