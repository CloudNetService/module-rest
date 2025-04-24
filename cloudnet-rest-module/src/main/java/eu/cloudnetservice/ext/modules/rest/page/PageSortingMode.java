/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.modules.rest.page;

import eu.cloudnetservice.utils.base.StringUtil;
import java.util.Comparator;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public enum PageSortingMode {

  ASC {
    @Override
    public @NonNull <T, K extends Comparable<K>> Comparator<T> comparator(@NonNull Function<T, K> extractor) {
      return Comparator.comparing(extractor);
    }
  },
  DESC {
    @Override
    public @NonNull <T, K extends Comparable<K>> Comparator<T> comparator(@NonNull Function<T, K> extractor) {
      return ASC.comparator(extractor).reversed();
    }
  };

  public static @Nullable PageSortingMode findSortingMode(@NonNull String name) {
    return switch (StringUtil.toLower(name)) {
      case "asc" -> ASC;
      case "desc" -> DESC;
      default -> null;
    };
  }

  public abstract <T, K extends Comparable<K>> @NonNull Comparator<T> comparator(@NonNull Function<T, K> extractor);
}
