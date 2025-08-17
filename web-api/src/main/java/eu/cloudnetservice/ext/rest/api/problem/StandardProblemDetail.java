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

package eu.cloudnetservice.ext.rest.api.problem;

import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import java.net.URI;

/**
 * Some standard problem details that only need initialization once and don't hold request-specific information.
 *
 * @since 1.0
 */
public final class StandardProblemDetail {

  /**
   * A problem describing the generic internal server error.
   */
  public static final ProblemDetail INTERNAL_SERVER_ERROR = ProblemDetail.builder()
    .title("Internal Server Error")
    .type(URI.create("internal-server-error"))
    .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
    .detail("An internal error has occurred. For admins: check the log for details, for users: please retry later.")
    .build();

  private StandardProblemDetail() {
    throw new UnsupportedOperationException();
  }
}
