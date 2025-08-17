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

package eu.cloudnetservice.ext.rest.api.response;

import lombok.NonNull;

/**
 * A wrapper object to construct a response from an object.
 *
 * @param <T> the type of the response body.
 * @since 1.0
 */
public interface IntoResponse<T> {

  /**
   * Constructs the inner response data for this object.
   *
   * @return the response data for the object that implements this interface.
   */
  default @NonNull Response<T> intoResponse() {
    return this.intoResponseBuilder().build();
  }

  /**
   * Constructs a response builder from this object.
   *
   * @return a builder for a response containing information from this object.
   */
  @NonNull Response.Builder<T, ?> intoResponseBuilder();
}
