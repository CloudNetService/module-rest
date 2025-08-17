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

package eu.cloudnetservice.ext.rest.api.codec;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;

/**
 * A codec loader that resolves {@link DataformatCodec} implementations using the {@link ServiceLoader} from Java
 * SPI.
 *
 * @see DataformatCodec
 * @see ServiceLoader
 * @since 1.0
 */
public final class CodecLoader {

  private static final Map<Class<?>, Object> CODEC_RESOLVE_CACHE = new ConcurrentHashMap<>(16, 0.9f, 1);

  private CodecLoader() {
    throw new UnsupportedOperationException();
  }

  /**
   * Loads a codec using the given type from Java SPI. After a codec for the given type was found the value is cached
   * and is used for any further calls for the same type.
   *
   * @param type the type of the codec.
   * @param <T>  the generic type of the codec.
   * @return the resolved codec for the given type.
   * @throws NullPointerException     if the given type is null.
   * @throws IllegalArgumentException if no codec for the given type was found.
   */
  @SuppressWarnings("unchecked")
  public static @NonNull <T extends DataformatCodec> T resolveCodec(@NonNull Class<T> type) {
    return (T) CODEC_RESOLVE_CACHE.computeIfAbsent(
      type,
      __ -> ServiceLoader.load(type, type.getClassLoader())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing codec implementation for: " + type.getSimpleName())));
  }
}
