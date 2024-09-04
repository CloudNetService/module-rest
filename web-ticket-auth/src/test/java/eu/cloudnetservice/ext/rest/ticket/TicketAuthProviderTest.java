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
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.auth.AuthTokenGenerationResult;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public final class TicketAuthProviderTest {

  private static Mac hashFunction;

  @BeforeAll
  static void setup() throws NoSuchAlgorithmException, InvalidKeyException {
    var mac = Mac.getInstance("HmacSHA256");
    var keyGen = KeyGenerator.getInstance("HmacSHA256");
    mac.init(keyGen.generateKey());

    hashFunction = mac;
  }

  @Test
  void testSuccessfulAuthFlow() throws InterruptedException {
    var authMock = mockBaseAuthRequest(null);
    var userManagement = authMock.management();
    var context = authMock.context();

    var userId = UUID.randomUUID();
    var scopes = Set.of("scope:test_scope", "scope:other_scope");

    var userMock = Mockito.mock(RestUser.class);
    Mockito.when(userMock.id()).thenReturn(userId);
    Mockito.when(userMock.hasScope(Mockito.anyString())).thenReturn(true);

    Mockito.when(userManagement.restUser(userId)).thenReturn(userMock);

    var authProvider = new TicketAuthProvider(Duration.ofSeconds(1), hashFunction);
    var generationResult = authProvider.generateAuthToken(userManagement, userMock, scopes);
    Assertions.assertInstanceOf(AuthTokenGenerationResult.Success.class, generationResult);

    var successfulResult = (AuthTokenGenerationResult.Success<?>) generationResult;
    var ticket = (TicketAuthToken) successfulResult.authToken();
    Assertions.assertEquals(scopes, ticket.scopes());
    Assertions.assertEquals(userId, ticket.userId());

    // set the newly generated ticket for the auth request
    Mockito.when(context.request().queryParameters()).thenReturn(Map.of("ticket", List.of(ticket.token())));

    // we pretend the user does not exist
    Mockito.when(userManagement.restUser(userId)).thenReturn(null);

    var authResult = authProvider.tryAuthenticate(context, userManagement, scopes);
    Assertions.assertEquals(AuthenticationResult.Constant.USER_NOT_FOUND, authResult);

    // the rest user should exist now, but misses some required scopes
    Mockito.when(userManagement.restUser(userId)).thenReturn(userMock);
    Mockito.when(userMock.hasScope(Mockito.anyString())).thenReturn(false);

    authResult = authProvider.tryAuthenticate(context, userManagement, scopes);
    Assertions.assertEquals(AuthenticationResult.Constant.MISSING_REQUIRED_SCOPES, authResult);

    // successful auth now
    Mockito.when(userMock.hasScope(Mockito.anyString())).thenReturn(true);
    authResult = authProvider.tryAuthenticate(context, userManagement, scopes);
    Assertions.assertInstanceOf(AuthenticationResult.Success.class, authResult);

    var successfulAuthResult = (AuthenticationResult.Success) authResult;
    Assertions.assertEquals(successfulAuthResult.restUser().id(), userId);
    Assertions.assertNull(successfulAuthResult.tokenId());

    // wait a bit and ensure that the ticket is invalid
    Thread.sleep(1000);
    var failedAuthResult = authProvider.tryAuthenticate(context, userManagement, scopes);
    Assertions.assertEquals(AuthenticationResult.Constant.INVALID_CREDENTIALS, failedAuthResult);
  }

  @Test
  void testQueryParameterEmptyMapReturnsProceed() {
    var authMock = mockBaseAuthRequest(null);
    var authProvider = new TicketAuthProvider(Duration.ofSeconds(1), hashFunction);

    Mockito.when(authMock.context.request().queryParameters()).thenReturn(Map.of());

    var result = authProvider.tryAuthenticate(authMock.context(), authMock.management(), Set.of());
    Assertions.assertEquals(AuthenticationResult.Constant.PROCEED, result);
  }

  @Test
  void testQueryParameterMissingReturnsProceed() {
    var authMock = mockBaseAuthRequest(null);
    var authProvider = new TicketAuthProvider(Duration.ofSeconds(1), hashFunction);

    var result = authProvider.tryAuthenticate(authMock.context(), authMock.management(), Set.of());
    Assertions.assertEquals(AuthenticationResult.Constant.PROCEED, result);
  }

  @Test
  void testInvalidTicketSignatureReturnsInvalidCredentials() {
    var authMock = mockBaseAuthRequest("MTcyMDgxMDM5OT=.1733b62e15c5");
    var authProvider = new TicketAuthProvider(Duration.ofSeconds(1), hashFunction);

    var result = authProvider.tryAuthenticate(authMock.context(), authMock.management(), Set.of());
    Assertions.assertEquals(AuthenticationResult.Constant.INVALID_CREDENTIALS, result);
  }

  @Test
  void testInvalidTicketFormatReturnsInvalidCredentials() {
    var data = TicketSecurityUtil.signTicket(hashFunction, "wrongTicketFormatData");
    var authMock = mockBaseAuthRequest(data);
    var authProvider = new TicketAuthProvider(Duration.ofSeconds(1), hashFunction);

    var result = authProvider.tryAuthenticate(authMock.context(), authMock.management(), Set.of());
    Assertions.assertEquals(AuthenticationResult.Constant.INVALID_CREDENTIALS, result);
  }

  @Test
  void testTicketGenerationWithMissingScopes() {
    var authMock = mockBaseAuthRequest(null);
    var userManagement = authMock.management();

    var userId = UUID.randomUUID();
    var scopes = Set.of("scope:test_scope", "scope:other_scope");

    var userMock = Mockito.mock(RestUser.class);
    Mockito.when(userMock.id()).thenReturn(userId);
    Mockito.when(userMock.hasScope(Mockito.anyString())).thenReturn(false);
    Mockito.when(userMock.hasScope(Mockito.eq("scope:other_scope"))).thenReturn(true);

    Mockito.when(userManagement.restUser(userId)).thenReturn(userMock);

    var authProvider = new TicketAuthProvider(Duration.ofSeconds(1), hashFunction);
    var generationResult = authProvider.generateAuthToken(userManagement, userMock, scopes);
    Assertions.assertEquals(AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES, generationResult);
  }

  private static AuthRequest mockBaseAuthRequest(String ticket) {
    var management = Mockito.mock(RestUserManagement.class);
    var context = Mockito.mock(HttpContext.class);
    var request = Mockito.mock(HttpRequest.class);
    var queryParams = ticket == null ? List.<String>of() : List.of(ticket);

    Mockito.when(context.request()).thenReturn(request);
    Mockito.when(request.queryParameters()).thenReturn(Map.of("ticket", queryParams));
    return new AuthRequest(management, context);
  }

  record AuthRequest(RestUserManagement management, HttpContext context) {

  }
}
