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

package eu.cloudnetservice.ext.modules.rest.page;

import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;

public class Paging {

  public static final String TOTAL_COUNT_HEADER = "X-Total-Count";
  public static final String RESULT_COUNT_HEADER = "X-Result-Count";

  public static <T, K extends Comparable<K>> JsonResponse.@NonNull Builder<?> pagedJsonResponse(
    @NonNull String mapKey,
    @NonNull Collection<T> data,
    @NonNull Function<T, K> extractor,
    @NonNull PageSortingMode sortingMode,
    int limit,
    int offset
  ) {
    var totalCount = data.size();
    var pagedData = data.stream().skip(offset).limit(limit).sorted(sortingMode.comparator(extractor)).toList();
    var resultCount = pagedData.size();

    return JsonResponse.builder()
      .body(Map.of(mapKey, pagedData))
      .header(TOTAL_COUNT_HEADER, Integer.toString(totalCount))
      .header(RESULT_COUNT_HEADER, Integer.toString(resultCount));
  }
}
