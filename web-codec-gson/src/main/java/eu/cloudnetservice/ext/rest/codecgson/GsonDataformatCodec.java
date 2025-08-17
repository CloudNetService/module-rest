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

package eu.cloudnetservice.ext.rest.codecgson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.cloudnetservice.ext.rest.api.codec.builtin.JsonCodec;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import lombok.NonNull;

public class GsonDataformatCodec implements JsonCodec {

  private static final Gson DEFAULT_GSON = new GsonBuilder()
    .serializeNulls()
    .disableHtmlEscaping()
    .create();

  private final Gson gson;

  public GsonDataformatCodec() {
    this(DEFAULT_GSON);
  }

  public GsonDataformatCodec(@NonNull Gson gson) {
    this.gson = gson;
  }

  @Override
  public @NonNull String serialize(@NonNull Type type, @NonNull Object object) {
    return this.gson.toJson(object, type);
  }

  @Override
  public @NonNull Object deserialize(@NonNull Charset charset, @NonNull Type objectType, @NonNull InputStream content) {
    try (var reader = new InputStreamReader(content, charset)) {
      return this.gson.fromJson(reader, objectType);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }
}
