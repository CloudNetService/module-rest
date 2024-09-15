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

package eu.cloudnetservice.ext.modules.rest;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.ext.modules.rest.auth.DefaultRestUser;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.basic.BasicAuthProvider;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.Regex;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.context.CommandInput;

@Singleton
@Permission("cloudnet.command.rest")
@Description("module-rest-command-description")
public final class RestCommand {

  private final AuthProvider authProvider;
  private final RestUserManagement restUserManagement;

  @Inject
  public RestCommand(@NonNull RestUserManagement restUserManagement) {
    this.restUserManagement = restUserManagement;
    this.authProvider = AuthProviderLoader.resolveAuthProvider("basic");
  }

  @Parser
  public @NonNull DefaultRestUser defaultRestUserParser(@NonNull CommandInput input) {
    var username = input.readString();
    var user = this.restUserManagement.restUserByUsername(username);
    if (user == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-rest-user-not-found", username));
    }

    // this implementation is based on our rest user implementation
    if (user instanceof DefaultRestUser defaultRestUser) {
      return defaultRestUser;
    }

    throw new ArgumentNotAvailableException(I18n.trans(
      "module-rest-unknown-user-type",
      user.getClass().getSimpleName()));
  }

  @Parser(name = "restUserScope")
  public @NonNull String restUserScopeParser(@NonNull CommandInput input) {
    var scope = input.readString();
    if (RestUser.SCOPE_NAMING_PATTERN.matcher(scope).matches()) {
      return scope;
    }

    throw new ArgumentNotAvailableException(I18n.trans(
      "argument-parse-failure-regex",
      RestUser.SCOPE_NAMING_PATTERN.pattern(),
      scope));
  }

  @Command("rest user create <username> <password>")
  public void createRestUser(
    @NonNull CommandSource source,
    @Argument("username") @Regex(DefaultRestUser.USER_NAMING_REGEX) @NonNull String username,
    @Argument("password") @Regex(DefaultRestUser.PASSWORD_REGEX) @NonNull String password
  ) {
    if (this.restUserManagement.restUserByUsername(username) != null) {
      source.sendMessage(I18n.trans("module-rest-user-already-existing", username));
      return;
    }

    // use the default rest user here as it provides a password setter
    var user = DefaultRestUser.builder()
      .createdAt(OffsetDateTime.now())
      .createdBy(source.name())
      .username(username)
      .password(password)
      .build();
    this.restUserManagement.saveRestUser(user);

    source.sendMessage(I18n.trans("module-rest-user-create-successful", username));
  }

  @Command("rest user delete <username>")
  public void deleteRestUser(@NonNull CommandSource source, @Argument("username") @NonNull DefaultRestUser restUser) {
    this.restUserManagement.deleteRestUser(restUser.id());
    source.sendMessage(I18n.trans("module-rest-user-delete-successful", restUser.username()));
  }

  @Command("rest user <username>")
  public void displayUser(@NonNull CommandSource source, @Argument("username") @NonNull DefaultRestUser restUser) {
    source.sendMessage("RestUser " + restUser.id() + ":" + restUser.username());
    source.sendMessage("Scopes:");
    for (var scope : restUser.scopes()) {
      source.sendMessage(" - " + scope);
    }
  }

  @Command("rest user <username> add scope <scope>")
  public void addScope(
    @NonNull CommandSource source,
    @Argument("username") @NonNull DefaultRestUser restUser,
    @Argument(value = "scope", parserName = "restUserScope") @NonNull String scope
  ) {
    this.updateRestUser(source, restUser, builder -> builder.scope(scope));
    source.sendMessage(I18n.trans("module-rest-user-add-scope-successful", restUser.username(), scope));
  }

  @Command("rest user <username> clear scopes")
  public void clearScopes(@NonNull CommandSource source, @Argument("username") @NonNull DefaultRestUser restUser) {
    this.updateRestUser(source, restUser, builder -> builder.scopes(Set.of()));
    source.sendMessage(I18n.trans("module-rest-user-clear-scopes-successful", restUser.username()));
  }

  @Command("rest user <username> remove scope <scope>")
  public void removeScope(
    @NonNull CommandSource source,
    @Argument("username") @NonNull DefaultRestUser restUser,
    @Argument("scope") @NonNull String scope
  ) {
    this.updateRestUser(source, restUser, builder -> builder.modifyScopes(scopes -> scopes.remove(scope)));
    source.sendMessage(I18n.trans("module-rest-user-remove-scope-successful", restUser.username(), scope));
  }

  @Command("rest user <username> set password <password>")
  public void setPassword(
    @NonNull CommandSource source,
    @Argument("username") @NonNull DefaultRestUser restUser,
    @Argument("password") @NonNull String password
  ) {
    this.updateRestUser(source, restUser, builder -> builder.password(password));
    source.sendMessage(I18n.trans("module-rest-user-password-changed", restUser.username()));
  }

  @Command("rest user <username> verifyPassword <password>")
  public void verifyPassword(
    @NonNull CommandSource source,
    @Argument("username") @NonNull DefaultRestUser restUser,
    @Argument("password") @NonNull String password
  ) {
    if (this.authProvider instanceof BasicAuthProvider basicAuthProvider) {
      var valid = basicAuthProvider.validatePassword(restUser, password.getBytes(StandardCharsets.UTF_8));
      if (valid) {
        source.sendMessage(I18n.trans("module-rest-user-password-match", restUser.username()));
      } else {
        source.sendMessage(I18n.trans("module-rest-user-password-mismatch", restUser.username()));
      }
    } else {
      source.sendMessage(I18n.trans("module-rest-user-verify-basic-auth-provider-missing"));
    }
  }

  private void updateRestUser(
    @NonNull CommandSource source,
    @NonNull DefaultRestUser user,
    @NonNull Consumer<DefaultRestUser.Builder> consumer
  ) {
    var builder = DefaultRestUser.builder(user);
    consumer.accept(builder);
    this.restUserManagement.saveRestUser(builder.modifiedAt(OffsetDateTime.now()).modifiedBy(source.name()).build());
  }
}
