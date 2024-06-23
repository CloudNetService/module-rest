/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.modules.rest.auth;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.ext.modules.rest.UUIDv7;
import eu.cloudnetservice.ext.modules.rest.auth.util.PasswordEncryptionUtil;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

// don't make this a record, the constructor of this class should be sealed
@SuppressWarnings("ClassCanBeRecord")
public final class DefaultRestUser implements RestUser {

  public static final String PASSWORD_REGEX = "^[\\w!€#$%&()*+,\\-./:;<=>?@\\[\\\\\\]^_`{|}~£]{6,128}$";
  public static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

  public static final String PASSWORD_KEY = "password";
  public static final String PASSWORD_SALT_KEY = "salt";

  private final UUID id;
  private final String username;
  private final Set<String> scopes;

  private final String createdBy;
  private final OffsetDateTime createdAt;

  private final String modifiedBy;
  private final OffsetDateTime modifiedAt;

  private final Map<String, String> properties;

  private DefaultRestUser(
    @NonNull UUID id,
    @NonNull String username,
    @NonNull Set<String> scopes,
    @NonNull String createdBy,
    @NonNull OffsetDateTime createdAt,
    @NonNull String modifiedBy,
    @NonNull OffsetDateTime modifiedAt,
    @NonNull Map<String, String> properties
  ) {
    this.id = id;
    this.username = username;
    this.scopes = scopes;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.modifiedBy = modifiedBy;
    this.modifiedAt = modifiedAt;
    this.properties = properties;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull RestUser user) {
    return builder()
      .id(user.id())
      .scopes(user.scopes())
      .username(user.username())
      .createdAt(user.createdAt())
      .createdBy(user.createdBy())
      .modifiedAt(user.modifiedAt())
      .modifiedBy(user.modifiedBy())
      .properties(user.properties());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasScope(@NonNull String scope) {
    return this.scopes.contains(RestUser.GLOBAL_ADMIN_SCOPE) || this.scopes.contains(StringUtil.toLower(scope));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<String> scopes() {
    return this.scopes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull UUID id() {
    return this.id;
  }

  @Override
  public @NonNull String username() {
    return this.username;
  }

  @Override
  public @NonNull OffsetDateTime createdAt() {
    return this.createdAt;
  }

  @Override
  public @NonNull String createdBy() {
    return this.createdBy;
  }

  @Override
  public @NonNull OffsetDateTime modifiedAt() {
    return this.modifiedAt;
  }

  @Override
  public @NonNull String modifiedBy() {
    return this.modifiedBy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> properties() {
    return this.properties;
  }

  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    return JsonResponse.<Map<String, Object>>builder().body(Map.of(
      "id", this.id,
      "username", this.username,
      "scopes", this.scopes,
      "createdAt", this.createdAt,
      "createdBy", this.createdBy,
      "modifiedAt", this.modifiedAt,
      "modifiedBy", this.modifiedBy));
  }

  public static final class Builder implements RestUser.Builder {

    private UUID id = UUIDv7.generate(System.currentTimeMillis());
    private String username;

    private Set<String> scopes = new HashSet<>();

    private String createdBy;
    private OffsetDateTime createdAt;

    private String modifiedBy;
    private OffsetDateTime modifiedAt;

    private Map<String, String> properties = new HashMap<>();

    @NonNull
    Builder id(@NonNull UUID id) {
      this.id = id;
      return this;
    }

    @Override
    public @NonNull Builder username(@NonNull String username) {
      this.validateUsername(username);
      this.username = username;
      return this;
    }

    public @NonNull Builder password(@NonNull String password) {
      this.validatePassword(password);
      var hashedPasswordInfo = PasswordEncryptionUtil.encrypt(password);
      this.properties.put(PASSWORD_KEY, hashedPasswordInfo.second());
      this.properties.put(PASSWORD_SALT_KEY, hashedPasswordInfo.first());
      return this;
    }

    @Override
    public @NonNull Builder property(@NonNull String key, @Nullable String value) {
      if (value != null) {
        this.properties.put(key, value);
      }
      return this;
    }

    @Override
    public @NonNull Builder properties(@NonNull Map<String, String> properties) {
      this.properties = new HashMap<>(properties);
      return this;
    }

    @Override
    public @NonNull Builder modifyProperties(@NonNull Consumer<Map<String, String>> modifier) {
      modifier.accept(this.properties);
      return this;
    }

    @Override
    public @NonNull Builder scope(@NonNull String scope) {
      this.validateScope(scope);
      this.scopes.add(scope);
      return this;
    }

    @Override
    public @NonNull Builder scopes(@NonNull Collection<String> scopes) {
      this.modifyScopes(currentScopes -> {
        currentScopes.clear();
        currentScopes.addAll(scopes);
      });
      return this;
    }

    @Override
    public @NonNull Builder modifyScopes(@NonNull Consumer<Collection<String>> modifier) {
      // copies the scope two times to validate that all scopes names are correct and to prevent
      //   1. mutation of the builder and then throwing an exception because of an illegal scope name (1. copy)
      //   2. mutation of the scope collection after checking that all names are valid (2. copy)
      var modifiableScopes = new HashSet<>(this.scopes);
      modifier.accept(modifiableScopes);

      modifiableScopes.forEach(this::validateScope);
      this.scopes = new HashSet<>(modifiableScopes);
      return this;
    }

    @Override
    public @NonNull Builder createdAt(@NonNull OffsetDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    @Override
    public @NonNull Builder createdBy(@NonNull String createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    @Override
    public @NonNull Builder modifiedAt(@NonNull OffsetDateTime modifiedAt) {
      this.modifiedAt = modifiedAt;
      return this;
    }

    @Override
    public @NonNull Builder modifiedBy(@NonNull String modifiedBy) {
      this.modifiedBy = modifiedBy;
      return this;
    }

    @Override
    public @NonNull RestUser build() {
      Preconditions.checkNotNull(this.id, "Missing rest user id");
      Preconditions.checkNotNull(this.username, "Missing rest user name");
      Preconditions.checkNotNull(this.createdAt, "Missing rest user creation time");
      Preconditions.checkNotNull(this.createdBy, "Missing rest user created by");

      Preconditions.checkArgument(this.properties.containsKey(PASSWORD_KEY), "Missing rest user password");
      Preconditions.checkArgument(this.properties.containsKey(PASSWORD_SALT_KEY), "Missing rest user salt key");

      return new DefaultRestUser(
        this.id,
        this.username,
        Set.copyOf(this.scopes),
        this.createdBy,
        this.createdAt,
        Objects.requireNonNullElse(this.modifiedBy, this.createdBy),
        Objects.requireNonNullElse(this.modifiedAt, this.createdAt),
        Map.copyOf(this.properties));
    }

    private void validatePassword(@NonNull String password) {
      var passwordMatcher = PASSWORD_PATTERN.matcher(password);
      if (!passwordMatcher.matches()) {
        throw new IllegalArgumentException(String.format(
          "Password does not fulfill the password requirements (Regex: %s)",
          PASSWORD_REGEX));
      }
    }

    private void validateScope(@Nullable String scope) {
      if (scope == null) {
        // might be the case when someone adds null into the scopes set during modification
        throw new IllegalArgumentException("Scope name cannot be null");
      }

      var scopeNameMatcher = RestUser.SCOPE_NAMING_PATTERN.matcher(scope);
      if (!scopeNameMatcher.matches()) {
        throw new IllegalArgumentException(String.format(
          "Scope %s does not fulfil the scope naming requirements (Regex: %s)",
          scope,
          RestUser.SCOPE_NAMING_PATTERN.pattern()));
      }
    }

    private void validateUsername(@NonNull String username) {
      var usernameMatcher = RestUser.USER_NAMING_PATTERN.matcher(username);
      if (!usernameMatcher.matches()) {
        throw new IllegalArgumentException(String.format(
          "Username %s does not fulfill the user naming requirements (Regex: %s)",
          username,
          RestUser.SCOPE_NAMING_PATTERN.pattern()));
      }
    }
  }
}
