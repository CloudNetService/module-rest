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

package eu.cloudnetservice.ext.rest.api.config;

import com.google.common.base.Strings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record CorsConfig(
  @NonNull List<Pattern> allowedOrigins,
  @NonNull List<String> allowedHeaders,
  @NonNull List<String> exposedHeaders,
  @Nullable Boolean allowCredentials,
  @Nullable Boolean allowPrivateNetworks,
  long maxAge
) {

  private static final Pattern ALLOW_ALL = Pattern.compile(".*");

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull CorsConfig config) {
    return builder()
      .allowedOrigins(config.allowedOrigins())
      .allowedHeaders(config.allowedHeaders())
      .exposedHeaders(config.exposedHeaders())
      .allowCredentials(config.allowCredentials())
      .allowPrivateNetworks(config.allowPrivateNetworks())
      .maxAge(Duration.ofSeconds(config.maxAge()));
  }

  private static <T> @NonNull List<T> combine(@NonNull List<T> left, @NonNull List<T> right, T permitAll) {
    if (left.contains(permitAll) || right.contains(permitAll)) {
      return List.of(permitAll);
    }

    Set<T> combined = new HashSet<>(left.size() + right.size());
    combined.addAll(left);
    combined.addAll(right);
    return new ArrayList<>(combined);
  }

  private static <T> @Nullable T combine(@Nullable T specific, @Nullable T global) {
    return specific != null ? specific : global;
  }

  private static @NonNull String trimTrailingSlash(@NonNull String input) {
    return input.endsWith("/") ? input.substring(0, input.length() - 1) : input;
  }

  public @NonNull CorsConfig combine(@Nullable CorsConfig other) {
    if (other == null) {
      return this;
    }

    return builder(this)
      .allowedOrigins(combine(this.allowedOrigins, other.allowedOrigins(), ALLOW_ALL))
      .allowedHeaders(combine(this.allowedHeaders, other.allowedHeaders(), "*"))
      .exposedHeaders(combine(this.exposedHeaders, other.exposedHeaders(), "*"))
      .allowCredentials(combine(this.allowCredentials, other.allowCredentials()))
      .allowPrivateNetworks(combine(this.allowPrivateNetworks, other.allowPrivateNetworks()))
      .maxAge(Duration.ofSeconds(this.maxAge))
      .build();
  }

  public @Nullable String findMatchingOrigin(@Nullable String origin) {
    if (this.allowedOrigins.isEmpty() || Strings.isNullOrEmpty(origin)) {
      return null;
    }

    var trimmedOrigin = trimTrailingSlash(origin);
    for (var originPattern : this.allowedOrigins) {
      if (originPattern.matcher(trimmedOrigin).matches()) {
        return trimmedOrigin;
      }
    }

    return null;
  }

  public @Nullable Collection<String> filterHeaders(@NonNull Collection<String> requestedHeaders) {
    if (requestedHeaders.isEmpty()) {
      return List.of();
    }

    if (this.allowedHeaders.isEmpty()) {
      return null;
    }

    List<String> results = new ArrayList<>();
    var allowAll = this.allowedHeaders.contains("*");
    for (var requestedHeader : requestedHeaders) {
      if (!requestedHeader.isBlank()) {
        var trimmedHeader = requestedHeader.trim();
        if (allowAll) {
          // all headers are allowed, no need to check further
          results.add(trimmedHeader);
        } else {
          // check if the requested header is part of the allowed headers
          for (var allowedHeader : this.allowedHeaders) {
            if (allowedHeader.equalsIgnoreCase(trimmedHeader)) {
              results.add(trimmedHeader);
              break;
            }
          }
        }
      }
    }

    return results.isEmpty() ? null : results;
  }

  public static final class Builder {

    private List<Pattern> allowedOrigins = new ArrayList<>();
    private List<String> allowedHeaders = new ArrayList<>();
    private List<String> exposedHeaders = new ArrayList<>();
    private Boolean allowCredentials;
    private Boolean allowPrivateNetworks;
    private Duration maxAge = Duration.ofSeconds(-1);

    public @NonNull Builder addAllowedOrigin(@NonNull String allowedOrigin) {
      Pattern originPattern;
      if (allowedOrigin.equals("*")) {
        // special case: the user wants to allow all origins to be allowed - therefore we need to
        // compile into a pattern that matches all origins
        originPattern = Pattern.compile(".*");
      } else {
        // escape the origin input before compiling as we want to literally match the input string
        originPattern = Pattern.compile(Pattern.quote(allowedOrigin));
      }

      return this.addAllowedOrigin(originPattern);
    }

    public @NonNull Builder addAllowedOrigin(@NonNull Pattern allowedOrigin) {
      this.allowedOrigins.add(allowedOrigin);
      return this;
    }

    public @NonNull Builder allowedOrigins(@NonNull List<Pattern> allowedOrigins) {
      this.allowedOrigins = new ArrayList<>(allowedOrigins);
      return this;
    }

    public @NonNull Builder modifyAllowedOrigins(@NonNull Consumer<List<Pattern>> allowedOriginConsumer) {
      allowedOriginConsumer.accept(this.allowedOrigins);
      return this;
    }

    public @NonNull Builder addAllowedHeader(@NonNull String allowedHeader) {
      this.allowedHeaders.add(allowedHeader);
      return this;
    }

    public @NonNull Builder allowedHeaders(@NonNull List<String> allowedHeaders) {
      this.allowedHeaders = new ArrayList<>(allowedHeaders);
      return this;
    }

    public @NonNull Builder modifyAllowedHeaders(@NonNull Consumer<List<String>> allowedHeaderConsumer) {
      allowedHeaderConsumer.accept(this.allowedHeaders);
      return this;
    }

    public @NonNull Builder addExposedHeader(@NonNull String exposedHeader) {
      this.exposedHeaders.add(exposedHeader);
      return this;
    }

    public @NonNull Builder exposedHeaders(@NonNull List<String> exposedHeaders) {
      this.exposedHeaders = new ArrayList<>(exposedHeaders);
      return this;
    }

    public @NonNull Builder modifyExposedHeaders(@NonNull Consumer<List<String>> exposedHeaderConsumer) {
      exposedHeaderConsumer.accept(this.exposedHeaders);
      return this;
    }

    public @NonNull Builder allowCredentials(@Nullable Boolean allowCredentials) {
      this.allowCredentials = allowCredentials;
      return this;
    }

    public @NonNull Builder allowPrivateNetworks(@Nullable Boolean allowPrivateNetworks) {
      this.allowPrivateNetworks = allowPrivateNetworks;
      return this;
    }

    public @NonNull Builder maxAge(@Nullable Duration maxAge) {
      this.maxAge = maxAge;
      return this;
    }

    public @NonNull CorsConfig build() {
      return new CorsConfig(
        List.copyOf(this.allowedOrigins),
        List.copyOf(this.allowedHeaders),
        List.copyOf(this.exposedHeaders),
        this.allowCredentials,
        this.allowPrivateNetworks,
        this.maxAge.toSeconds());
    }
  }

}
