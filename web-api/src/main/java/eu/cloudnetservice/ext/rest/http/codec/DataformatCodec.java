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

package eu.cloudnetservice.ext.rest.http.codec;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import lombok.NonNull;

public interface DataformatCodec {

  @NonNull String serialize(@NonNull Type type, @NonNull Object object);

  @NonNull Object deserialize(@NonNull Charset charset, @NonNull Type objectType, @NonNull InputStream content);
}
