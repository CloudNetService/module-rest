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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.ext.modules.rest.auth.DefaultRestUser;
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
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

@Singleton
public final class V3HttpHandlerUser {

  private static final Type SET_STRING_TYPE = TypeFactory.parameterizedClass(Set.class, String.class);

  private final RestUserManagement restUserManagement;

  @Inject
  public V3HttpHandlerUser(@NonNull RestUserManagement restUserManagement) {
    this.restUserManagement = restUserManagement;
  }

  @RequestHandler(path = "/api/v3/user", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleRestUserCreation(
    @NonNull @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:user_write", "cloudnet_rest:user_create"}) RestUser user,
    @NonNull @RequestTypedBody Document body
  ) {
    var id = body.getString("id");
    var password = body.getString("password");
    Set<String> scopes = body.readObject("scopes", SET_STRING_TYPE);

    if (id == null || scopes == null || password == null) {
      return ProblemDetail.builder()
        .type("invalid-rest-user-body")
        .title("Invalid Rest User Body")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain the required properties.");
    }

    if (this.restUserManagement.restUser(id) != null) {
      return ProblemDetail.builder()
        .type("rest-user-already-exists")
        .title("Rest User Already Exists")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("There already is a rest user with the provided id: %s".formatted(id));
    }

    // setting scopes while creating a user is only allowed for users with the global admin scope
    if (!scopes.isEmpty() && !user.hasScope(RestUser.GLOBAL_ADMIN_SCOPE)) {
      return ProblemDetail.builder()
        .type("missing-permission-rest-user-scopes")
        .title("Missing Permission Rest User Scopes")
        .status(HttpResponseCode.FORBIDDEN)
        .detail("Creating a rest user with scopes requires the scope: %s".formatted(RestUser.GLOBAL_ADMIN_SCOPE));
    }

    for (var scope : scopes) {
      if (!this.checkScopeValidity(scope)) {
        return ProblemDetail.builder()
          .type("invalid-rest-user-scope")
          .title("Invalid Rest User Scope")
          .status(HttpResponseCode.BAD_REQUEST)
          .detail("The scope %s does not match the scope pattern %s.".formatted(
            scope,
            RestUser.SCOPE_NAMING_PATTERN.pattern()));
      }
    }

    var constructedUser = DefaultRestUser.builder().id(id).scopes(scopes).password(password).build();
    this.restUserManagement.saveRestUser(constructedUser);

    return JsonResponse.builder().body(constructedUser);
  }

  @RequestHandler(path = "/api/v3/user/{uniqueId}", method = HttpMethod.GET)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:user_read", "cloudnet_rest:user_get"})
  public @NonNull IntoResponse<?> handleGetUser(@NonNull @RequestPathParam("uniqueId") String id) {
    var restUser = this.restUserManagement.restUser(id);
    if (restUser == null) {
      return ProblemDetail.builder()
        .type("rest-user-not-found")
        .title("Rest User Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail("There is no rest user with the provided id: %s".formatted(id));
    }

    var strippedUser = this.restUserManagement.builder(restUser).properties(Map.of()).build();
    return JsonResponse.builder().body(strippedUser);
  }

  @RequestHandler(path = "/api/v3/user/{uniqueId}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:user_write", "cloudnet_rest:user_delete"})
  public @NonNull IntoResponse<?> handleDeleteUser(@NonNull @RequestPathParam("uniqueId") String id) {
    this.restUserManagement.deleteRestUser(id);
    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/user/{uniqueId}", method = HttpMethod.PUT)

  public @NonNull IntoResponse<?> handleUpdateUser(
    @NonNull @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:user_write", "cloudnet_rest:user_update"}) RestUser requestSender,
    @NonNull @RequestPathParam("uniqueId") String id,
    @NonNull @RequestTypedBody Document body
  ) {
    var updateId = body.getString("id");
    var password = body.getString("password");
    Set<String> scopes = body.readObject("scopes", SET_STRING_TYPE);

    // at least one value has to be present so that we can do an update
    if (updateId == null && scopes == null && password == null) {
      return ProblemDetail.builder()
        .type("missing-rest-user-update")
        .title("Missing Rest User Update")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain any information to update.");
    }

    // these modification will require extra permissions if the user is not editing himself
    var ownUser = id.equals(requestSender.id());
    if (!ownUser && (updateId != null || password != null)) {
      if (!requestSender.hasScope(RestUser.GLOBAL_ADMIN_SCOPE)) {
        return ProblemDetail.builder()
          .type("missing-rest-user-update-scopes")
          .title("Missing Rest User Update Scopes")
          .status(HttpResponseCode.FORBIDDEN)
          .detail("The requested action needs further access rights (%s)".formatted(RestUser.GLOBAL_ADMIN_SCOPE));
      }
    }

    var restUser = this.restUserManagement.restUser(id);
    if (restUser == null) {
      return ProblemDetail.builder()
        .type("rest-user-not-found")
        .title("Rest User Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail("There is no rest user with the provided id: %s".formatted(id));
    }

    if (updateId != null && this.restUserManagement.restUser(updateId) != null) {
      return ProblemDetail.builder()
        .type("rest-user-already-exists")
        .title("Rest User Already Exists")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("There already is a rest user with the provided id: %s".formatted(id));
    }

    var builder = DefaultRestUser.builder(restUser);
    if (updateId != null) {
      builder.id(updateId);
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

      for (var scope : scopes) {
        if (!this.checkScopeValidity(scope)) {
          return ProblemDetail.builder()
            .type("invalid-rest-user-scope")
            .title("Invalid Rest User Scope")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail("The scope %s does not match the scope pattern %s.".formatted(
              scope,
              RestUser.SCOPE_NAMING_PATTERN.pattern()));
        }
      }

      builder.scopes(scopes);
    }

    // delete the old user as an update is just delete -> recreate
    this.restUserManagement.deleteRestUser(id);

    var updatedUser = builder.build();
    this.restUserManagement.saveRestUser(updatedUser);

    // we don't want to display the properties (contains password and jwt infos)
    return JsonResponse.builder().body(builder.properties(Map.of()).build());
  }

  private boolean checkScopeValidity(@NonNull String scope) {
    var scopeNameMatcher = RestUser.SCOPE_NAMING_PATTERN.matcher(scope);
    return scopeNameMatcher.matches();
  }
}
