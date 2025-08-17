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

package eu.cloudnetservice.ext.rest.api;

import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.Response;
import java.io.Serial;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This exception type allows to be thrown in case of any handling exception, that should properly be propagated to the
 * client with a response. Other exceptions (exceptions that are not extending this type and are not implementing
 * {@link IntoResponse}) are left to be processed by a
 * {@link eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor}.
 * <p>
 * Http handle exceptions are always stackless and the cause of them cannot be changed after constructing an instance of
 * them. Passed information (such as the detail message or stack trace) are not exposed to client and should be used for
 * server-side debugging of issues only.
 *
 * @since 1.0
 */
public class HttpHandleException extends RuntimeException implements IntoResponse<Object> {

  @Serial
  private static final long serialVersionUID = 2647591868821053340L;

  protected final transient IntoResponse<?> response;

  /**
   * Constructs a new http handle exception. This exception type allows to be thrown in case of any handling exception,
   * that should properly be propagated to the client with a response. Other exceptions (exceptions that are not
   * extending this type and are not implementing {@link IntoResponse}) are left to be processed by a
   * {@link eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor}.
   *
   * @param response the response to send to the client.
   * @throws NullPointerException if the given response is null.
   */
  public HttpHandleException(@NonNull IntoResponse<?> response) {
    this(response, null, null);
  }

  /**
   * Constructs a new http handle exception. This exception type allows to be thrown in case of any handling exception,
   * that should properly be propagated to the client with a response. Other exceptions (exceptions that are not
   * extending this type and are not implementing {@link IntoResponse}) are left to be processed by a
   * {@link eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor}.
   * <p>
   * The message and cause parameters can optionally be specified for debug reasons. They are not exposed to the
   * client.
   *
   * @param response the response to send to the client.
   * @param message  an optional detail message of the error cause (for service side debugging only).
   * @param cause    an optional cause why the error occurred (for service side debugging only).
   * @throws NullPointerException if the given response is null.
   */
  public HttpHandleException(
    @NonNull IntoResponse<?> response,
    @Nullable String message,
    @Nullable Throwable cause
  ) {
    super(message, cause);
    this.response = response;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final @NonNull Throwable fillInStackTrace() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final @NonNull Throwable initCause(@Nullable Throwable cause) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void setStackTrace(@NonNull StackTraceElement[] stackTrace) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public final @NonNull Response<Object> intoResponse() {
    return (Response<Object>) this.response.intoResponse();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public final @NonNull Response.Builder<Object, ?> intoResponseBuilder() {
    return (Response.Builder<Object, ?>) this.response.intoResponseBuilder();
  }
}
