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

package eu.cloudnetservice.ext.rest.api.codec.builtin;

import eu.cloudnetservice.ext.rest.api.codec.CodecLoader;
import eu.cloudnetservice.ext.rest.api.codec.DataformatCodec;

/**
 * A dataformat codec that supports both serialization of a POJO to a string representation and back to a POJO. This
 * json codec specifies that every implementation serializes into json and deserializes from json.
 * <p>
 * To obtain a codec implementation use {@code CodecProvider.resolveCodec(JsonCodec.class)}
 *
 * @see CodecLoader
 * @see DataformatCodec
 * @since 1.0
 */
public interface JsonCodec extends DataformatCodec {

}
