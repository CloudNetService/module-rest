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

package eu.cloudnetservice.ext.rest.ticket;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthTokenGenerationResult;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.ScopedRestUserDelegate;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import lombok.NonNull;

public class TicketAuthProvider implements AuthProvider {

  private static final String QUERY_PARAMETER_NAME = "ticket";

  private static final Duration DEFAULT_WEBSOCKET_TICKET_EXPIRATION = Duration.ofSeconds(30);
  private static final Mac DEFAULT_MAC_FUNCTION;

  static {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      var keyGen = KeyGenerator.getInstance("HmacSHA256");
      mac.init(keyGen.generateKey());

      DEFAULT_MAC_FUNCTION = mac;
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private final Duration ticketDuration;
  private final Mac hashFunction;

  public TicketAuthProvider() {
    this(DEFAULT_WEBSOCKET_TICKET_EXPIRATION, DEFAULT_MAC_FUNCTION);
  }

  public TicketAuthProvider(@NonNull Duration ticketDuration, @NonNull Mac hashFunction) {
    this.hashFunction = hashFunction;
    this.ticketDuration = ticketDuration;
  }

  @Override
  public int priority() {
    return AuthProvider.DEFAULT_PRIORITY;
  }

  @Override
  public boolean supportsTokenGeneration() {
    return true;
  }

  @Override
  public @NonNull String name() {
    return "ticket";
  }

  @Override
  public @NonNull AuthTokenGenerationResult generateAuthToken(
    @NonNull RestUserManagement management,
    @NonNull RestUser restUser,
    @NonNull Set<String> scopes
  ) {
    for (var scope : scopes) {
      if (!restUser.hasScope(scope)) {
        // the user requested a ticket with scopes the user is not allowed to use
        return AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES;
      }
    }

    return new AuthTokenGenerationResult.Success<>(this.generateTicket(restUser.id(), scopes));
  }

  @Override
  public @NonNull AuthenticationResult tryAuthenticate(
    @NonNull HttpContext context,
    @NonNull RestUserManagement management,
    @NonNull Set<String> requiredScopes
  ) {
    // check if there is at least one query parameter "ticket"
    var ticketQueryParameters = context.request().queryParameters().get(QUERY_PARAMETER_NAME);
    if (ticketQueryParameters.isEmpty()) {
      return AuthenticationResult.Constant.PROCEED;
    }

    // only allow tickets that were not tampered with
    var ticketToken = ticketQueryParameters.getFirst();
    if (!TicketSecurityUtil.verifyTicketSignature(this.hashFunction, ticketToken)) {
      return AuthenticationResult.Constant.INVALID_CREDENTIALS;
    }

    // ensure that we can parse the ticket
    var ticket = TicketAuthToken.parseTicket(ticketToken);
    if (ticket == null) {
      return AuthenticationResult.Constant.INVALID_CREDENTIALS;
    }

    var user = management.restUser(ticket.userId());
    if (user == null) {
      return AuthenticationResult.Constant.USER_NOT_FOUND;
    }

    // check that the ticket is not expired
    var expirationTime = ticket.creationTime().plus(this.ticketDuration);
    if (expirationTime.isBefore(Instant.now())) {
      return AuthenticationResult.Constant.INVALID_CREDENTIALS;
    }

    // wrap the user to ensure that only the scopes in the jwt are used
    var scopedUser = new ScopedRestUserDelegate(user, ticket.scopes());
    // ensure to only pass if the user has one of the required scopes
    if (!scopedUser.hasOneScopeOf(requiredScopes)) {
      return AuthenticationResult.Constant.MISSING_REQUIRED_SCOPES;
    }

    return new AuthenticationResult.Success(scopedUser, null);
  }

  private @NonNull TicketAuthToken generateTicket(@NonNull UUID userId, @NonNull Set<String> scopes) {
    var creationTime = Instant.now();
    var builder = new StringJoiner(TicketAuthToken.PROPERTY_DELIMITER);
    builder.add(Long.toString(creationTime.getEpochSecond()));
    builder.add(userId.toString());
    if (!scopes.isEmpty()) {
      builder.add(String.join(TicketAuthToken.SCOPE_DELIMITER, scopes));
    }

    var token = TicketSecurityUtil.signTicket(this.hashFunction, builder.toString());
    return new TicketAuthToken(userId, creationTime, token, scopes);
  }
}
