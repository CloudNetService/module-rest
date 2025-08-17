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

import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import java.io.InputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents a http message sent from or to the server. The common use case is a http request and response.
 *
 * @param <T> the generic type of the class implementing this interface.
 * @see HttpRequest
 * @see HttpResponse
 * @since 1.0
 */
public interface HttpMessage<T extends HttpMessage<T>> extends HttpCookieAware<T> {

  /**
   * Gets the context this message is processed in.
   *
   * @return the associated context of this message.
   */
  @NonNull HttpContext context();

  /**
   * Gets the http header map containing all set headers for this http message.
   *
   * @return the http header map for this http message.
   */
  @NonNull HttpHeaderMap headers();

  /**
   * Gets the http version of this http message. CloudNet currently only supports Http 1 and Http 1.1.
   *
   * @return the version of this http message.
   */
  @NonNull HttpVersion version();

  /**
   * Sets the version of this http message.
   *
   * @param version the version to use.
   * @return the same message as used to call the method, for chaining.
   * @throws NullPointerException if the given http version is null.
   */
  @NonNull T version(@NonNull HttpVersion version);

  /**
   * Gets the body content of this http message converted to a byte array. If there is no need to read the body fully
   * into the heap, prefer {@link #bodyStream()} instead.
   *
   * @return the body content of this http message.
   */
  byte[] body();

  /**
   * Converts the body of the http message into an utf-8 encoded string.
   *
   * @return the body of the message converted to a string.
   */
  @NonNull String bodyAsString();

  /**
   * Sets the body of the http message. If there is no need to load the full body into the heap, prefer streaming the
   * content by using {@link #body(InputStream)} instead.
   *
   * @param byteArray the body as a byte array.
   * @return the same message as used to call the method, for chaining.
   * @throws UnsupportedOperationException if setting the body is not supported for the http message.
   */
  @NonNull T body(byte[] byteArray);

  /**
   * Sets the body to the given string. This might be useful to e.g. return a json response.
   *
   * @param text the body as a string.
   * @return the same message as used to call the method, for chaining.
   * @throws UnsupportedOperationException if setting the body is not supported for the http message.
   * @throws NullPointerException          if the given text is null.
   */
  @NonNull T body(@NonNull String text);

  /**
   * Gets the body as a streamable content source to reduce the heap load when reading the body content. This method
   * returns null if no http body is provided in the message.
   *
   * @return the body as a content stream, or null if the message has no http body.
   */
  @UnknownNullability InputStream bodyStream();

  /**
   * Sets the body of this http message to the given input stream. Closing of the stream will be done automatically and
   * should not be made after calling the method. Setting the body stream to null will remove the current body.
   *
   * @param body the new body stream to use.
   * @return the same message as used to call the method, for chaining.
   * @throws UnsupportedOperationException if setting the body is not supported for the http message.
   */
  @NonNull T body(@Nullable InputStream body);

  /**
   * Gets if the current http message has a body present.
   *
   * @return if a body is present.
   */
  boolean hasBody();
}
