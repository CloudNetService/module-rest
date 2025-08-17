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

package eu.cloudnetservice.ext.rest.api.response.type;

import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.api.response.Response;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A response that only has a status code and headers but no http body data.
 *
 * @see Response
 * @since 1.0
 */
public final class RawResponse extends DefaultResponse<Void> {

  private RawResponse(
    @NonNull HttpHeaderMap httpHeaderMap,
    @NonNull HttpResponseCode responseCode
  ) {
    super(null, httpHeaderMap, responseCode);
  }

  /**
   * Constructs a new, empty raw response builder.
   *
   * @return a new, empty raw response builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new response builder from the given response, copying over every value except for the body from the
   * given response.
   *
   * @param response the response to copy the values from.
   * @return a new raw response builder with the values from the given response already set.
   * @throws NullPointerException if the given response is null.
   */
  public static @NonNull Builder builder(@NonNull Response<?> response) {
    return builder().responseCode(response.responseCode()).header(response.headers());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull Void body) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Response.Builder<Void, ?> intoResponseBuilder() {
    return RawResponse.builder(this);
  }

  /**
   * A builder for a raw response.
   *
   * @see Response.Builder
   * @since 1.0
   */
  public static final class Builder extends DefaultResponseBuilder<Void, Builder> {

    private Builder() {
    }

    /**
     * This method does nothing as a raw response doesn't have a body.
     */
    @Override
    public @NonNull Builder body(@Nullable Void body) {
      // no-op
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Response<Void> build() {
      return new RawResponse(this.httpHeaderMap.unmodifiableClone(), this.responseCode);
    }
  }
}
