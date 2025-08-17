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
import eu.cloudnetservice.ext.rest.api.codec.CodecLoader;
import eu.cloudnetservice.ext.rest.api.codec.builtin.JsonCodec;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.api.response.Response;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The json response implementation which is capable of serializing a given object into the response for the request.
 * <p>
 * This implementation uses the {@link JsonCodec} to serialize, the implementation can be swapped by including a
 * different codec using the java service provider interface.
 *
 * @see Response
 * @see JsonCodec
 * @see java.util.ServiceLoader
 * @since 1.0
 */
public final class JsonResponse<T> extends DefaultResponse<T> {

  private JsonResponse(
    @Nullable T body,
    @NonNull HttpHeaderMap httpHeaderMap,
    @NonNull HttpResponseCode responseCode
  ) {
    super(body, httpHeaderMap, responseCode);
  }

  /**
   * Constructs a new empty json response builder.
   *
   * @return a new empty json response builder.
   */
  public static <T> @NonNull Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Constructs a new json response builder copying all values from the given response.
   *
   * @param response the response to copy the values from.
   * @return a new file response builder copying all values from the given response.
   * @throws NullPointerException if the given response is null.
   */
  public static <T> @NonNull Builder<T> builder(@NonNull Response<? extends T> response) {
    return JsonResponse.<T>builder()
      .responseCode(response.responseCode())
      .header(response.headers())
      .body(response.body());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull T body) {
    var codec = CodecLoader.resolveCodec(JsonCodec.class);
    response.body(codec.serialize(body.getClass(), body));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Response.Builder<T, ?> intoResponseBuilder() {
    return JsonResponse.builder(this);
  }

  /**
   * The json response builder implementation applying the last response specific build setup.
   *
   * @see eu.cloudnetservice.ext.rest.api.response.Response.Builder
   * @since 1.0
   */
  public static final class Builder<T> extends DefaultResponseBuilder<T, Builder<T>> {

    private Builder() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Response<T> build() {
      this.httpHeaderMap.setIfAbsent(HttpHeaders.CONTENT_TYPE, List.of(MediaType.JSON_UTF_8.toString()));
      return new JsonResponse<>(this.body, this.httpHeaderMap.unmodifiableClone(), this.responseCode);
    }
  }
}
