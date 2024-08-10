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

import eu.cloudnetservice.ext.modules.rest.dto.auth.ScopedJwtBody;
import eu.cloudnetservice.ext.modules.rest.dto.auth.ScopedTicketRequestBody;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.AuthTokenGenerationResult;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthProvider;
import eu.cloudnetservice.ext.rest.jwt.JwtTokenHolder;
import eu.cloudnetservice.ext.rest.jwt.JwtTokenPropertyParser;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@EnableValidation
public final class V3HttpHandlerAuthorization {

  private static final ProblemDetail AUTH_REQUESTED_INVALID_SCOPES = ProblemDetail.builder()
    .title("Auth Refresh Invalid Scope")
    .type(URI.create("auth-refresh-invalid-scope"))
    .status(HttpResponseCode.FORBIDDEN)
    .detail("The scoped refresh token contains a scope that is not valid anymore.")
    .build();
  private static final ProblemDetail TICKET_REQUESTED_INVALID_SCOPES = ProblemDetail.builder()
    .title("Ticket Creation Requested Invalid Scopes")
    .type(URI.create("ticket-creation-invalid-scopes"))
    .status(HttpResponseCode.FORBIDDEN)
    .detail("Requested scopes for the ticket that the user is not allowed to use.")
    .build();

  private final AuthProvider jwtAuthProvider;
  private final AuthProvider ticketAuthProvider;
  private final RestUserManagement userManagement;

  public V3HttpHandlerAuthorization() {
    this.jwtAuthProvider = AuthProviderLoader.resolveAuthProvider("jwt");
    this.ticketAuthProvider = AuthProviderLoader.resolveAuthProvider("ticket");

    this.userManagement = RestUserManagementLoader.load();
  }

  @RequestHandler(path = "/api/v3/auth", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleBasicAuthLoginRequest(
    @Authentication(providers = "basic") @NonNull RestUser user,
    @NonNull @Valid @RequestTypedBody ScopedJwtBody body
  ) {
    var scopes = Objects.requireNonNullElse(body.scopes(), Set.<String>of());
    var result = this.jwtAuthProvider.generateAuthToken(this.userManagement, user, scopes);
    return switch (result) {
      case AuthTokenGenerationResult.Success<?> success -> success.authToken();
      case AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES -> AUTH_REQUESTED_INVALID_SCOPES;
    };
  }

  @RequestHandler(path = "/api/v3/auth/ticket", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleTicketRequest(
    @NonNull @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:ticket_create", "cloudnet_rest:ticket_write"}) RestUser user,
    @NonNull @Valid @RequestTypedBody ScopedTicketRequestBody body
  ) {
    var generationResult = this.ticketAuthProvider.generateAuthToken(this.userManagement, user, body.scopes());
    return switch (generationResult) {
      case AuthTokenGenerationResult.Success<?> success -> success.authToken();
      case AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES -> TICKET_REQUESTED_INVALID_SCOPES;
    };
  }

  @RequestHandler(path = "/api/v3/auth/refresh", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleRefreshRequest(@NonNull HttpContext context) {
    var authenticationResult = this.jwtAuthProvider.tryAuthenticate(context, this.userManagement, Set.of());
    return switch (authenticationResult) {
      case AuthenticationResult.Success ignored -> ProblemDetail.builder()
        .type("access-token-used")
        .title("Access Token Used")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("This route can only be called with an refresh token, not with an access token");
      case AuthenticationResult.InvalidTokenType refreshResult -> {
        // remove the token was used to access (and now refresh)
        // the client must now use the new token returned by the call
        var user = refreshResult.restUser();
        var validTokens = JwtTokenPropertyParser.parseTokens(user.properties().get(JwtAuthProvider.JWT_TOKEN_PAIR_KEY))
          .stream()
          .filter(holder -> !holder.tokenId().equals(refreshResult.tokenId()))
          .toList();

        // re-compact the valid tokens & write the properties back to the user object
        var compactTokens = JwtTokenPropertyParser.compactTokens(validTokens);
        user = this.userManagement.builder(user).modifyProperties(properties -> {
          if (compactTokens == null) {
            // no tokens left, just remove the property
            properties.remove(JwtAuthProvider.JWT_TOKEN_PAIR_KEY);
          } else {
            // update the property
            properties.put(JwtAuthProvider.JWT_TOKEN_PAIR_KEY, compactTokens);
          }
        }).build();

        var authToken = this.jwtAuthProvider.generateAuthToken(this.userManagement, user, refreshResult.scopes());

        // CHECKSTYLE.OFF: checkstyle has a problem with nested switches
        // generate a new auth token for the user - this will also save the user with the new valid tokens
        yield switch (authToken) {
          case AuthTokenGenerationResult.Success<?> success -> success.authToken();
          case AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES -> AUTH_REQUESTED_INVALID_SCOPES;
        };
        // CHECKSTYLE.ON
      }
      default -> ProblemDetail.builder()
        .type("invalid-refresh-token")
        .title("Invalid Refresh Token")
        .status(HttpResponseCode.UNAUTHORIZED)
        .detail("The provided refresh token is invalid");
    };
  }

  @RequestHandler(path = "/api/v3/auth/verify", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleVerifyRequest(
    @NonNull HttpContext context,
    @NonNull @Valid @RequestTypedBody ScopedJwtBody body
  ) {
    var scopes = Objects.requireNonNullElse(body.scopes(), Set.<String>of());
    var authenticationResult = this.jwtAuthProvider.tryAuthenticate(context, this.userManagement, scopes);
    var convertedAuthResult = this.convertAuthResult(authenticationResult);
    if (convertedAuthResult == null) {
      return ProblemDetail.builder()
        .type("invalid-token")
        .title("Invalid Token")
        .status(HttpResponseCode.UNAUTHORIZED)
        .detail("The provided authentication token is invalid");
    }

    var compactedValidTokens = convertedAuthResult._1().properties().get(JwtAuthProvider.JWT_TOKEN_PAIR_KEY);
    var expiry = JwtTokenPropertyParser.parseTokens(compactedValidTokens)
      .stream()
      .filter(token -> token.tokenType().equals(convertedAuthResult._2()))
      .filter(token -> token.tokenId().equals(convertedAuthResult._3()))
      .findFirst()
      .map(JwtTokenHolder::expiresAt)
      .orElse(null);
    if (expiry == null) {
      return ProblemDetail.builder()
        .type("unknown-token")
        .title("Unknown Token")
        .status(HttpResponseCode.UNAUTHORIZED)
        .detail("The provided authentication token is unknown");
    }

    return JsonResponse.builder().body(Map.of("type", convertedAuthResult._2(), "expiresAt", expiry.toEpochMilli()));
  }

  @RequestHandler(path = "/api/v3/auth/revoke", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> revokeAuthToken(@NonNull HttpContext context) {
    var authenticationResult = this.jwtAuthProvider.tryAuthenticate(context, this.userManagement, Set.of());
    var convertedAuthResult = this.convertAuthResult(authenticationResult);
    if (convertedAuthResult == null) {
      return ProblemDetail.builder()
        .type("invalid-token")
        .title("Invalid Token")
        .status(HttpResponseCode.UNAUTHORIZED)
        .detail("The provided authentication token is invalid");
    }

    // filter out the token to invalidate & re-compute the valid token property
    var compactedValidTokens = convertedAuthResult._1().properties().get(JwtAuthProvider.JWT_TOKEN_PAIR_KEY);
    var validTokens = JwtTokenPropertyParser.parseTokens(compactedValidTokens)
      .stream()
      .filter(token -> !token.tokenId().equals(convertedAuthResult._3()))
      .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableList(), JwtTokenPropertyParser::compactTokens));

    // save the rest user with the token property removed or set to the new value
    var newRestUser = this.userManagement.builder(convertedAuthResult._1()).modifyProperties(properties -> {
      if (validTokens == null) {
        properties.remove(JwtAuthProvider.JWT_TOKEN_PAIR_KEY);
      } else {
        properties.put(JwtAuthProvider.JWT_TOKEN_PAIR_KEY, validTokens);
      }
    }).build();
    this.userManagement.saveRestUser(newRestUser);

    return HttpResponseCode.NO_CONTENT;
  }

  // do not expose, returning Tuple3 here is awful :)
  private @Nullable Tuple3<RestUser, String, String> convertAuthResult(@NonNull AuthenticationResult result) {
    return switch (result) {
      case AuthenticationResult.Success success ->
        Tuple.of(success.restUser(), JwtTokenHolder.ACCESS_TOKEN_TYPE, success.tokenId());
      case AuthenticationResult.InvalidTokenType invalidTokenType ->
        Tuple.of(invalidTokenType.restUser(), JwtTokenHolder.REFRESH_TOKEN_TYPE, invalidTokenType.tokenId());
      default -> null;
    };
  }
}
