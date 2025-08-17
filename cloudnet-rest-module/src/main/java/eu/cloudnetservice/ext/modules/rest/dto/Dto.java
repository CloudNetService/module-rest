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

package eu.cloudnetservice.ext.modules.rest.dto;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;

public interface Dto<T> {

  @NonNull T toEntity();

  static <T, D extends Dto<T>> Stream<T> toStream(@NonNull Collection<D> entries) {
    return entries.stream().map(Dto::toEntity);
  }

  static <T, D extends Dto<T>> List<T> toList(@NonNull Collection<D> entries) {
    return toStream(entries).toList();
  }
}
