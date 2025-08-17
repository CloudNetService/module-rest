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

import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.response.type.FileResponse;
import eu.cloudnetservice.ext.rest.api.response.type.InputStreamResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.api.response.type.PlainTextResponse;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a response from a {@link eu.cloudnetservice.ext.rest.api.HttpHandler} method containing all information
 * needed to respond to a request.
 * <p>
 * To construct a response the corresponding builder should be used.
 * <ul>
 *   <li>{@link JsonResponse#builder()} for responses that are serialized to json</li>
 *   <li>{@link FileResponse#builder()} for responses that have a file attachment</li>
 *   <li>{@link InputStreamResponse#builder()} for responses that have any kind of input stream</li>
 *   <li>{@link PlainTextResponse#builder()} for responses that just contain plain text</li>
 *   <li>Other implementations are possible by implementing this interface and the corresponding builder</li>
 * </ul>
 *
 * @param <T> the generic type of the body.
 * @since 1.0
 */
public interface Response<T> extends IntoResponse<T> {

  /**
   * Gets the body of this response. The body is serialized based on the used implementation of this response.
   *
   * @return the body of this response, null if no body was set.
   */
  @Nullable T body();

  /**
   * Gets the response code set for this response to sent back to the requester.
   *
   * @return the http response code.
   */
  @NonNull HttpResponseCode responseCode();

  /**
   * Gets the http header map of this response containing all headers that are sent back to the requester.
   * <p>
   * This header map is unmodifiable and modifications will result in an {@link UnsupportedOperationException}.
   *
   * @return the http header map of this response.
   */
  @Unmodifiable
  @NonNull HttpHeaderMap headers();

  /**
   * Serializes this response into the actual {@link HttpResponse} that is sent back.
   *
   * @param response the actual response to serialize into.
   * @throws NullPointerException if the given http response is null.
   */
  void serializeIntoResponse(@NonNull HttpResponse response);

  /**
   * A builder for a response.
   *
   * @param <T> the generic type of the body contained in the response.
   * @param <B> the generic type of the builder itself.
   * @see eu.cloudnetservice.ext.rest.api.response.type.JsonResponse.Builder
   * @see eu.cloudnetservice.ext.rest.api.response.type.FileResponse.Builder
   * @see eu.cloudnetservice.ext.rest.api.response.type.PlainTextResponse.Builder
   * @see eu.cloudnetservice.ext.rest.api.response.type.InputStreamResponse.Builder
   * @since 1.0
   */
  interface Builder<T, B extends Builder<T, B>> extends IntoResponse<T> {

    /**
     * Sets the response code of the response.
     *
     * @param responseCode the response code to respond with.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given response code is null.
     */
    @NonNull B responseCode(@NonNull HttpResponseCode responseCode);

    /**
     * Sets the response code of this response to {@link HttpResponseCode#NOT_FOUND}.
     * <p>
     * Essentially this is a short-cut for {@code responseCode(HttpResponseCode.NOT_FOUND)}.
     *
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull B notFound();

    /**
     * Sets the response code of this response to {@link HttpResponseCode#NO_CONTENT}.
     * <p>
     * Essentially this is a short-cut for {@code responseCode(HttpResponseCode.NO_CONTENT)}.
     *
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull B noContent();

    /**
     * Sets the response code of this response to {@link HttpResponseCode#BAD_REQUEST}.
     * <p>
     * Essentially this is a short-cut for {@code responseCode(HttpResponseCode.BAD_REQUEST)}.
     *
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull B badRequest();

    /**
     * Sets the response code of this response to {@link HttpResponseCode#FORBIDDEN}.
     * <p>
     * Essentially this is a short-cut for {@code responseCode(HttpResponseCode.FORBIDDEN)}.
     *
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull B forbidden();

    /**
     * Sets the given header map for this response replacing all previously set values.
     *
     * @param httpHeaderMap the header map to set.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given header map is null.
     */
    @NonNull B header(@NonNull HttpHeaderMap httpHeaderMap);

    /**
     * Adds the given header values to this response. Already set header with the same name are not replaced.
     *
     * @param name   the name of the header.
     * @param values the values for the header.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name, the array or any of the string array values is null.
     */
    @NonNull B header(@NonNull String name, @NonNull String... values);

    /**
     * Modifies the http header map for this response.
     *
     * @param headerModifier the modifier to apply to the http header map.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given modifier is null.
     */
    @NonNull B modifyHeaders(@NonNull Consumer<HttpHeaderMap> headerModifier);

    /**
     * Sets the {@link com.google.common.net.HttpHeaders#ETAG} header for this response and makes sure that the etag is
     * correctly wrapped in quotes.
     *
     * @param etag the etag value for the etag header.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given etag is null.
     */
    @NonNull B eTag(@NonNull String etag);

    /**
     * Sets the {@link com.google.common.net.HttpHeaders#LAST_MODIFIED} header for this response.
     * <p>
     * This short-cut method makes sure that the time is using {@code GMT} as time zone and serializes the date time
     * into the correct format as described in the <a
     * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified">specification</a>.
     *
     * @param lastModified the zoned date time providing the time to set in the header.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given date time is null.
     */
    @NonNull B lastModified(@NonNull ZonedDateTime lastModified);

    /**
     * Sets the {@link com.google.common.net.HttpHeaders#LAST_MODIFIED} header for this response.
     * <p>
     * This short-cut method makes sure that the time is using {@code GMT} as time zone and serializes the date time
     * into the correct format as described in the <a
     * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified">specification</a>.
     *
     * @param lastModified the instant providing the time to set in the header.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given date time is null.
     */
    @NonNull B lastModified(@NonNull Instant lastModified);

    /**
     * Sets the {@link com.google.common.net.HttpHeaders#LOCATION} header for this response.
     *
     * @param location the uri for the location header.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given uri is null.
     */
    @NonNull B location(@NonNull URI location);

    /**
     * Sets the {@link com.google.common.net.HttpHeaders#CONTENT_TYPE} header for this response.
     *
     * @param contentType the content type for the content type header.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given content type is null.
     */
    @NonNull B contentType(@NonNull String contentType);

    /**
     * Sets the {@link com.google.common.net.HttpHeaders#CONTENT_LENGTH} header for this response.
     *
     * @param contentLength the length for the content length header.
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull B contentLength(long contentLength);

    /**
     * Sets the body for this response that is serialized based on the implementation.
     *
     * @param body the body to include in the response.
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull B body(@Nullable T body);

    /**
     * Constructs a new response from this builder.
     *
     * @return the newly constructed response.
     */
    @NonNull Response<T> build();
  }
}
