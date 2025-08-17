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

import eu.cloudnetservice.ext.rest.api.HttpHandleException;
import java.io.Serial;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A http handle exception that responds with a problem detail description.
 *
 * @since 1.0
 */
public class ProblemHttpHandleException extends HttpHandleException {

  @Serial
  private static final long serialVersionUID = -4628868016653750683L;

  /**
   * Constructs a new handle exception with the given problem detail as the response body.
   *
   * @param response the problem detail to respond with.
   * @throws NullPointerException if the given problem response is null.
   */
  public ProblemHttpHandleException(@NonNull ProblemDetail response) {
    super(response);
  }

  /**
   * Constructs a new handle exception with the given problem detail as the response body and an optional message and
   * exception cause for debugging the issue.
   *
   * @param response the problem detail to respond with.
   * @param message  an optional detail message of the error cause (for service side debugging only).
   * @param cause    an optional cause why the error occurred (for service side debugging only).
   * @throws NullPointerException if the given response is null.
   */
  public ProblemHttpHandleException(
    @NonNull ProblemDetail response,
    @Nullable String message,
    @Nullable Throwable cause
  ) {
    super(response, message, cause);
  }
}
