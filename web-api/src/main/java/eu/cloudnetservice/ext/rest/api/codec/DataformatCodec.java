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

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import lombok.NonNull;

/**
 * A dataformat codec that supports both serialization of a POJO to a string representation and back to a POJO. To
 * obtain a codec implementation use {@link CodecLoader#resolveCodec(Class)} with the registered codec type.
 *
 * @see CodecLoader
 * @see eu.cloudnetservice.ext.rest.api.codec.builtin.JsonCodec
 * @since 1.0
 */
public interface DataformatCodec {

  /**
   * Serializes the given object from the given type into a string.
   *
   * @param type   the type of the object to serialize.
   * @param object the object to serialize.
   * @return the string representation of the given object.
   * @throws NullPointerException if the given type or object is null.
   */
  @NonNull String serialize(@NonNull Type type, @NonNull Object object);

  /**
   * Deserializes the given input stream to an object of the given type.
   *
   * @param charset    the charset to use when reading from the stream.
   * @param objectType the type of the object to deserialize to.
   * @param content    the content to deserialize into an object.
   * @return the deserialized object.
   * @throws NullPointerException if the given charset, type or content is null.
   */
  @NonNull Object deserialize(@NonNull Charset charset, @NonNull Type objectType, @NonNull InputStream content);
}
