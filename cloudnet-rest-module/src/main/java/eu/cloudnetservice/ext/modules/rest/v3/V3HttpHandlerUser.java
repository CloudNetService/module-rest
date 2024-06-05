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

package eu.cloudnetservice.ext.modules.rest.v3;

import eu.cloudnetservice.ext.modules.rest.UUIDv7;
import eu.cloudnetservice.ext.modules.rest.auth.DefaultRestUser;
import eu.cloudnetservice.ext.modules.rest.dto.user.RestUserDto;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.Response;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.NonNull;
import org.hibernate.validator.constraints.UUID;

@Singleton
@EnableValidation
public final class V3HttpHandlerUser {

  private final RestUserManagement restUserManagement;

  @Inject
  public V3HttpHandlerUser(@NonNull RestUserManagement restUserManagement) {
    this.restUserManagement = restUserManagement;
  }

  @RequestHandler(path = "/api/v3/user", method = HttpMethod.GET)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:user_read", "cloudnet_rest:user_get_all"})
  public @NonNull IntoResponse<?> handleRestUserList() {
    return JsonResponse.builder().body(Map.of("users", this.restUserManagement.users()
      .stream()
      .map(IntoResponse::intoResponse)
      .map(Response::body)
      .toList()));
  }

  @RequestHandler(path = "/api/v3/user", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleRestUserCreation(
    @NonNull @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:user_write", "cloudnet_rest:user_create"}) RestUser user,
    @NonNull @RequestTypedBody @Valid RestUserDto body
  ) {
    var scopes = body.scopes();
    var username = body.username();
    var password = body.password();
    if (username == null || scopes == null || password == null) {
      return ProblemDetail.builder()
        .type("invalid-rest-user-body")
        .title("Invalid Rest User Body")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain the required properties.");
    }

    if (this.restUserManagement.restUserByUsername(username) != null) {
      return ProblemDetail.builder()
        .type("rest-user-already-exists")
        .title("Rest User Already Exists")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("There already is a rest user with the provided username: %s".formatted(username));
    }

    // setting scopes while creating a user is only allowed for users with the global admin scope
    if (!scopes.isEmpty() && !user.hasScope(RestUser.GLOBAL_ADMIN_SCOPE)) {
      return ProblemDetail.builder()
        .type("missing-permission-rest-user-scopes")
        .title("Missing Permission Rest User Scopes")
        .status(HttpResponseCode.FORBIDDEN)
        .detail("Creating a rest user with scopes requires the scope: %s".formatted(RestUser.GLOBAL_ADMIN_SCOPE));
    }

    var constructedUser = DefaultRestUser.builder()
      .scopes(scopes)
      .username(username)
      .createdAt(OffsetDateTime.now())
      .createdBy(user.username())
      .password(password)
      .build();
    this.restUserManagement.saveRestUser(constructedUser);

    return constructedUser;
  }

  @RequestHandler(path = "/api/v3/user/{uniqueId}", method = HttpMethod.GET)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:user_read", "cloudnet_rest:user_get"})
  public @NonNull IntoResponse<?> handleGetUser(
    @NonNull @RequestPathParam("uniqueId") @UUID(version = 7) String id
  ) {
    var restUser = this.restUserManagement.restUser(java.util.UUID.fromString(id));
    if (restUser == null) {
      return ProblemDetail.builder()
        .type("rest-user-not-found")
        .title("Rest User Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail("There is no rest user with the provided id: %s".formatted(id));
    }

    return restUser;
  }

  @RequestHandler(path = "/api/v3/user/{uniqueId}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:user_write", "cloudnet_rest:user_delete"})
  public @NonNull IntoResponse<?> handleDeleteUser(
    @NonNull @RequestPathParam("uniqueId") @UUID(version = 7) String id
  ) {
    this.restUserManagement.deleteRestUser(UUIDv7.fromString(id));
    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/user/{uniqueId}", method = HttpMethod.PUT)
  public @NonNull IntoResponse<?> handleUpdateUser(
    @NonNull @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:user_write", "cloudnet_rest:user_update"}) RestUser requestSender,
    @NonNull @RequestPathParam("uniqueId") @UUID(version = 7) String id,
    @NonNull @RequestTypedBody @Valid RestUserDto body
  ) {
    var uniqueId = UUIDv7.fromString(id);

    var scopes = body.scopes();
    var username = body.username();
    var password = body.password();

    // at least one value has to be present so that we can do an update
    if (username == null && scopes == null && password == null) {
      return ProblemDetail.builder()
        .type("missing-rest-user-update")
        .title("Missing Rest User Update")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain any information to update.");
    }

    // these modification will require extra permissions if the user is not editing himself
    var ownUser = uniqueId.equals(requestSender.id());
    if (!ownUser && (username != null || password != null)) {
      if (!requestSender.hasScope(RestUser.GLOBAL_ADMIN_SCOPE)) {
        return ProblemDetail.builder()
          .type("missing-rest-user-update-scopes")
          .title("Missing Rest User Update Scopes")
          .status(HttpResponseCode.FORBIDDEN)
          .detail("The requested action needs further access rights (%s)".formatted(RestUser.GLOBAL_ADMIN_SCOPE));
      }
    }

    var restUser = this.restUserManagement.restUser(uniqueId);
    if (restUser == null) {
      return ProblemDetail.builder()
        .type("rest-user-not-found")
        .title("Rest User Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail("There is no rest user with the provided id: %s".formatted(id));
    }

    if (username != null && this.restUserManagement.restUserByUsername(username) != null) {
      return ProblemDetail.builder()
        .type("rest-user-already-exists")
        .title("Rest User Already Exists")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("There already is a rest user with the provided username: %s".formatted(username));
    }

    var builder = DefaultRestUser.builder(restUser);
    if (username != null) {
      builder.username(username);
    }

    if (password != null) {
      builder.password(password);
    }

    if (scopes != null) {
      if (!requestSender.hasScope(RestUser.GLOBAL_ADMIN_SCOPE)) {
        return ProblemDetail.builder()
          .type("missing-permission-rest-user-scopes")
          .title("Missing Permission Rest User Scopes")
          .status(HttpResponseCode.FORBIDDEN)
          .detail("Creating a rest user with scopes requires the scope: %s".formatted(RestUser.GLOBAL_ADMIN_SCOPE));
      }

      builder.scopes(scopes);
    }

    var user = builder.modifiedAt(OffsetDateTime.now()).modifiedBy(requestSender.username()).build();
    this.restUserManagement.saveRestUser(user);
    return user;
  }
}
