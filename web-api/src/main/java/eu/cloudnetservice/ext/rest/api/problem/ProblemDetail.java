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

import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * An implementation of the problem details response body described in <a
 * href="https://datatracker.ietf.org/doc/html/rfc9457">RFC 9457</a>.
 *
 * @since 1.0
 */
public final class ProblemDetail implements IntoResponse<Map<String, Object>> {

  private static final String PROBLEM_JSON_CONTENT_TYPE = MediaType.create("application", "problem+json")
    .withCharset(StandardCharsets.UTF_8)
    .toString();

  private final URI type;
  private final String title;
  private final String detail;
  private final String instance;
  private final HttpResponseCode status;
  private final Map<String, Object> additionalFields;

  private ProblemDetail(
    @Nullable URI type,
    @Nullable String title,
    @Nullable String detail,
    @Nullable String instance,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, Object> additionalFields
  ) {
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.instance = instance;
    this.status = responseCode;
    this.additionalFields = additionalFields;
  }

  /**
   * Constructs a new empty problem detail builder.
   *
   * @return a new empty problem detail builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new problem detail builder copying all values from the given problem detail.
   *
   * @param problemDetail the problem detail to copy the values from.
   * @return a new problem detail builder copying all values from the given problem detail.
   * @throws NullPointerException if the given problem detail is null.
   */
  public static @NonNull Builder builder(@NonNull ProblemDetail problemDetail) {
    return builder()
      .type(problemDetail.type())
      .title(problemDetail.title())
      .detail(problemDetail.detail())
      .status(problemDetail.status())
      .instance(problemDetail.instance())
      .additionalFields(problemDetail.additionalFields());
  }

  /**
   * Gets the status code of the problem detail.
   *
   * @return the status code of the problem detail.
   */
  public @NonNull HttpResponseCode status() {
    return this.status;
  }

  /**
   * Gets the optional type of the problem detail. The type is an uri used to identify the problem type.
   *
   * @return the optional type of the problem detail, null if not given.
   */
  public @Nullable URI type() {
    return this.type;
  }

  /**
   * Gets the optional title of the problem detail. The title is a short human-readable summary of the problem.
   *
   * @return the optional title of the problem detail, null if not given.
   */
  public @Nullable String title() {
    return this.title;
  }

  /**
   * Gets the optional detail of the problem detail. The detail is a longer explanation for the problem that occurred.
   *
   * @return the optional detail of the problem detail, null if not given.
   */
  public @Nullable String detail() {
    return this.detail;
  }

  /**
   * Gets the optional instance of the problem. The instance is any string that points to a specific instance or call
   * that caused the problem.
   *
   * @return the optional instance of the problem, null if not given.
   */
  public @Nullable String instance() {
    return this.instance;
  }

  /**
   * Gets an unmodifiable map with all additional fields of the problem detail. The map might contain information like a
   * timestamp.
   *
   * @return an unmodifiable map with all additional fields of the problem detail.
   */
  @Unmodifiable
  public @NonNull Map<String, Object> additionalFields() {
    return this.additionalFields;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The problem detail is always serialized into json and flattened before doing so.
   */
  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    // serializes the members of the problem details body
    // see https://datatracker.ietf.org/doc/html/rfc9457#section-3.1
    Map<String, Object> serializedProblemDetail = new HashMap<>();
    this.registerFieldToSerialize(serializedProblemDetail, "type", this.type);
    this.registerFieldToSerialize(serializedProblemDetail, "title", this.title);
    this.registerFieldToSerialize(serializedProblemDetail, "status", this.status.code());
    this.registerFieldToSerialize(serializedProblemDetail, "detail", this.detail);
    this.registerFieldToSerialize(serializedProblemDetail, "instance", this.instance);
    serializedProblemDetail.putAll(this.additionalFields);

    return JsonResponse.<Map<String, Object>>builder()
      .responseCode(this.status)
      .contentType(PROBLEM_JSON_CONTENT_TYPE)
      .body(Map.copyOf(serializedProblemDetail));
  }

  /**
   * Registers the key-value pair to the given map if the value is present.
   *
   * @param properties the map to insert the pair into.
   * @param key        the key.
   * @param value      the value.
   * @throws NullPointerException if the given map or key is null.
   */
  private void registerFieldToSerialize(
    @NonNull Map<String, Object> properties,
    @NonNull String key,
    @Nullable Object value
  ) {
    if (value != null) {
      properties.put(key, value);
    }
  }

  /**
   * A builder for a problem detail.
   *
   * @since 1.0
   */
  public static final class Builder implements IntoResponse<Map<String, Object>> {

    private Map<String, Object> additionalFields = new HashMap<>();
    private HttpResponseCode status = HttpResponseCode.INTERNAL_SERVER_ERROR;

    private URI type;
    private String title;
    private String detail;
    private String instance;

    /**
     * Sets the status code for the problem detail which is passed into the response.
     *
     * @param statusCode the response code.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given response code is null.
     */
    public @NonNull Builder status(@NonNull HttpResponseCode statusCode) {
      this.status = statusCode;
      return this;
    }

    /**
     * Sets the optional type of the problem detail. The type is an uri used to identify the problem type.
     *
     * @param type the optional type of the problem detail.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder type(@Nullable URI type) {
      this.type = type;
      return this;
    }

    /**
     * Sets the optional type of the problem detail. While this method takes a string, it is converted to an uri that is
     * used to identify the problem type.
     *
     * @param type the optional type of the problem detail.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given type is null.
     * @throws IllegalArgumentException if the given type is not a valid uri.
     */
    public @NonNull Builder type(@NonNull String type) {
      return this.type(URI.create(type));
    }

    /**
     * Sets the optional title of the problem detail. The title is a short human-readable summary of the problem.
     *
     * @param title the optional title of the problem detail.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder title(@Nullable String title) {
      this.title = title;
      return this;
    }

    /**
     * Sets the optional detail of the problem detail. The detail is a longer explanation for the problem that
     * occurred.
     *
     * @param detail the optional detail of the problem detail.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder detail(@Nullable String detail) {
      this.detail = detail;
      return this;
    }

    /**
     * Sets the optional instance of the problem. The instance is any string that points to a specific instance or call
     * that caused the problem.
     *
     * @param instance the optional instance of the problem.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder instance(@Nullable String instance) {
      this.instance = instance;
      return this;
    }

    /**
     * Adds an additional field to the problem detail.
     *
     * @param key   the key of the additional field.
     * @param value the value of the additional field.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key or value is null.
     */
    public @NonNull Builder addAdditionalField(@NonNull String key, @NonNull Object value) {
      this.additionalFields.put(key, value);
      return this;
    }

    /**
     * Sets the additional fields of the problem detail, replacing already set fields.
     *
     * @param additionalFields the fields to set.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given field map is null.
     */
    public @NonNull Builder additionalFields(@NonNull Map<String, Object> additionalFields) {
      this.additionalFields = new HashMap<>(additionalFields);
      return this;
    }

    /**
     * Modifies the additional fields of the problem detail.
     *
     * @param modifier the additional field modifier.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given modifier is null.
     */
    public @NonNull Builder modifyAdditionalFields(@NonNull Consumer<Map<String, Object>> modifier) {
      modifier.accept(this.additionalFields);
      return this;
    }

    /**
     * Constructs a new problem detail from this builder.
     *
     * @return a new problem detail.
     */
    public @NonNull ProblemDetail build() {
      return new ProblemDetail(
        this.type,
        this.title,
        this.detail,
        this.instance,
        this.status,
        Map.copyOf(this.additionalFields));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
      return this.build().intoResponseBuilder();
    }
  }
}
