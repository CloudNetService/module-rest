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

import eu.cloudnetservice.ext.modules.rest.dto.auth.ScopedWebSocketTicketBody;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.AuthTokenGenerationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.NonNull;

@Singleton
@EnableValidation
public final class V3HttpHandlerWebSocket {

  private static final ProblemDetail WS_REQUESTED_INVALID_SCOPES = ProblemDetail.builder()
    .title("WebSocket Ticket Creation Requested Invalid Scopes")
    .type(URI.create("websocket-ticket-creation-invalid-scopes"))
    .status(HttpResponseCode.FORBIDDEN)
    .detail("Requested scopes for the websocket tickets that the user is not allowed to use.")
    .build();

  private final AuthProvider<?> wsAuthProvider;
  private final RestUserManagement restUserManagement;

  public V3HttpHandlerWebSocket() {
    this.wsAuthProvider = AuthProviderLoader.resolveAuthProvider("websocket");
    this.restUserManagement = RestUserManagementLoader.load();
  }

  @RequestHandler(path = "/api/v3/websocket/ticket", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleWebSocketTicketRequest(
    @NonNull @Authentication(providers = "jwt", scopes = {"cloudnet_rest:websocket_ticket"}) RestUser user,
    @NonNull @Valid @RequestTypedBody ScopedWebSocketTicketBody body
  ) {
    var generationResult = this.wsAuthProvider.generateAuthToken(this.restUserManagement, user, body.scopes());
    return switch (generationResult) {
      case AuthTokenGenerationResult.Success<?> success -> success.authToken();
      case AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES -> WS_REQUESTED_INVALID_SCOPES;
    };
  }
}
