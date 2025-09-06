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

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.util.HttpDateUtil;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract default implementation of a response builder applying all values not bound to the body itself.
 *
 * @param <T> the generic type of the body.
 * @param <B> the generic type of the builder itself.
 * @see Response.Builder
 * @since 1.0
 */
public abstract class DefaultResponseBuilder<T, B extends Response.Builder<T, B>> implements Response.Builder<T, B> {

  protected T body;
  protected HttpResponseCode responseCode = HttpResponseCode.OK;
  protected HttpHeaderMap httpHeaderMap = HttpHeaderMap.newHeaderMap();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B responseCode(@NonNull HttpResponseCode responseCode) {
    this.responseCode = responseCode;
    return this.self();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B notFound() {
    return this.responseCode(HttpResponseCode.NOT_FOUND);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B noContent() {
    return this.responseCode(HttpResponseCode.NO_CONTENT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B badRequest() {
    return this.responseCode(HttpResponseCode.BAD_REQUEST);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B forbidden() {
    return this.responseCode(HttpResponseCode.FORBIDDEN);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B header(@NonNull HttpHeaderMap httpHeaderMap) {
    this.httpHeaderMap = HttpHeaderMap.newHeaderMap().set(httpHeaderMap);
    return this.self();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B header(@NonNull String name, @NonNull String... values) {
    this.httpHeaderMap.add(name, values);
    return this.self();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B modifyHeaders(@NonNull Consumer<HttpHeaderMap> headerModifier) {
    headerModifier.accept(this.httpHeaderMap);
    return this.self();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B eTag(@NonNull String etag) {
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
    if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
      etag = "\"" + etag;
    }
    if (!etag.endsWith("\"")) {
      etag += "\"";
    }

    return this.header(HttpHeaders.ETAG, etag);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B lastModified(@NonNull ZonedDateTime lastModified) {
    var formattedDate = HttpDateUtil.formatAsHttpDate(lastModified);
    return this.header(HttpHeaders.LAST_MODIFIED, formattedDate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B lastModified(@NonNull Instant lastModified) {
    var formattedDate = HttpDateUtil.formatAsHttpDate(lastModified);
    return this.header(HttpHeaders.LAST_MODIFIED, formattedDate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B location(@NonNull URI location) {
    return this.header(HttpHeaders.LOCATION, location.toASCIIString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B contentType(@NonNull String contentType) {
    return this.header(HttpHeaders.CONTENT_TYPE, contentType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B contentLength(long contentLength) {
    return this.header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull B body(@Nullable T body) {
    this.body = body;
    return this.self();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Response.Builder<T, ?> intoResponseBuilder() {
    return this;
  }

  @SuppressWarnings("unchecked")
  private @NonNull B self() {
    return (B) this;
  }
}
