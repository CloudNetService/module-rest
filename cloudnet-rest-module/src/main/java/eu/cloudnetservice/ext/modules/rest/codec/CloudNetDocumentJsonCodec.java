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

package eu.cloudnetservice.ext.modules.rest.codec;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.DocumentParseException;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.ext.rest.api.codec.builtin.JsonCodec;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import lombok.NonNull;

public final class CloudNetDocumentJsonCodec implements JsonCodec {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String serialize(@NonNull Type type, @NonNull Object object) {
    return Document.newJsonDocument().appendTree(object).serializeToString(StandardSerialisationStyle.COMPACT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object deserialize(@NonNull Charset charset, @NonNull Type objectType, @NonNull InputStream content) {
    try (var reader = new InputStreamReader(content, charset)) {
      return DocumentFactory.json().parse(reader).toInstanceOf(objectType);
    } catch (IOException exception) {
      throw new DocumentParseException(exception);
    }
  }
}
