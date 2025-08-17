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

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.api.response.Response;
import java.io.InputStream;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The input stream response implementation which is capable of serializing the given input stream into the response for
 * the request.
 *
 * @see Response
 * @since 1.0
 */
public final class InputStreamResponse extends DefaultResponse<InputStream> {

  private InputStreamResponse(
    @Nullable InputStream body,
    @NonNull HttpHeaderMap httpHeaderMap,
    @NonNull HttpResponseCode responseCode
  ) {
    super(body, httpHeaderMap, responseCode);
  }

  /**
   * Constructs a new empty input stream response builder.
   *
   * @return a new empty input stream response builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new input stream response builder copying all values from the given response.
   *
   * @param response the response to copy the values from.
   * @return a new file response builder copying all values from the given response.
   * @throws NullPointerException if the given response is null.
   */
  public static @NonNull Builder builder(@NonNull Response<InputStream> response) {
    return builder().responseCode(response.responseCode()).header(response.headers()).body(response.body());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull InputStream body) {
    response.body(body);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Response.Builder<InputStream, ?> intoResponseBuilder() {
    return InputStreamResponse.builder(this);
  }

  /**
   * The input stream response builder implementation applying the last response specific build setup.
   *
   * @see eu.cloudnetservice.ext.rest.api.response.Response.Builder
   * @since 1.0
   */
  public static final class Builder extends DefaultResponseBuilder<InputStream, Builder> {

    private Builder() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Response<InputStream> build() {
      this.httpHeaderMap.setIfAbsent(HttpHeaders.CONTENT_TYPE, List.of(MediaType.OCTET_STREAM.toString()));
      return new InputStreamResponse(this.body, this.httpHeaderMap.unmodifiableClone(), this.responseCode);
    }
  }
}
