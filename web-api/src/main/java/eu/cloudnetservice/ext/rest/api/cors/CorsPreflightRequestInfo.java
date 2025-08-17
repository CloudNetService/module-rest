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

package eu.cloudnetservice.ext.rest.api.cors;

import java.util.List;
import lombok.NonNull;

/**
 * A cors preflight request info holder providing essential information when working with CORS and preflight requests.
 *
 * @param origin         the origin that sent the preflight request.
 * @param requestMethod  the requested http method.
 * @param requestHeaders the requested headers.
 * @see CorsRequestProcessor
 * @since 1.0
 */
public record CorsPreflightRequestInfo(
  @NonNull String origin,
  @NonNull String requestMethod,
  @NonNull List<String> requestHeaders
) {

}
