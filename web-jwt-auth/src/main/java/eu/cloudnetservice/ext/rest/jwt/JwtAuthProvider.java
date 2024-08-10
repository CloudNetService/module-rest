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

package eu.cloudnetservice.ext.rest.jwt;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonSyntaxException;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthTokenGenerationResult;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.ScopedRestUserDelegate;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class JwtAuthProvider implements AuthProvider {

  public static final String JWT_TOKEN_PAIR_KEY = "jwt_token_pair";

  private static final String DEFAULT_ISSUER = "global_issuer";
  private static final Duration DEFAULT_ACCESS_TOKEN_EXPIRATION = Duration.ofHours(10);
  private static final Duration DEFAULT_REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

  private static final Pattern BEARER_LOGIN_PATTERN = Pattern.compile("Bearer ([a-zA-Z\\d-_.]+)$");

  private final String issuer;
  private final Key jwtSigningKey;

  private final Duration accessDuration;
  private final Duration refreshDuration;

  private final JwtParser jwtParser;

  public JwtAuthProvider() {
    this(
      DEFAULT_ISSUER,
      Jwts.SIG.HS512.key().build(),
      null,
      DEFAULT_ACCESS_TOKEN_EXPIRATION,
      DEFAULT_REFRESH_TOKEN_EXPIRATION);
  }

  public JwtAuthProvider(
    @NonNull String issuer,
    @NonNull KeyPair jwtSigningKeys,
    @NonNull Duration accessDuration,
    @NonNull Duration refreshDuration
  ) {
    this(issuer, jwtSigningKeys.getPrivate(), jwtSigningKeys.getPublic(), accessDuration, refreshDuration);
  }

  public JwtAuthProvider(
    @NonNull String issuer,
    @NonNull Key jwtSigningKey,
    @Nullable Key jwtValidationKey,
    @NonNull Duration accessDuration,
    @NonNull Duration refreshDuration
  ) {
    this.issuer = issuer;
    this.jwtSigningKey = jwtSigningKey;
    this.accessDuration = accessDuration;
    this.refreshDuration = refreshDuration;

    // symmetric keys only use one key for signing and validating
    // asymmetric keys need a separate keys: one to sign and one to validate
    var validationKey = Objects.requireNonNullElse(jwtValidationKey, jwtSigningKey);
    if (validationKey instanceof SecretKey validationSecretKey) {
      this.jwtParser = Jwts.parser().requireIssuer(issuer).verifyWith(validationSecretKey).build();
    } else if (validationKey instanceof PublicKey validationPublicKey) {
      this.jwtParser = Jwts.parser().requireIssuer(issuer).verifyWith(validationPublicKey).build();
    } else {
      throw new IllegalArgumentException("Verify key must either be a SecretKey (for MAC algorithms) or a PublicKey");
    }
  }

  @Override
  public boolean supportsTokenGeneration() {
    return true;
  }

  @Override
  public int priority() {
    return AuthProvider.DEFAULT_PRIORITY;
  }

  @Override
  public @NonNull String name() {
    return "jwt";
  }

  @Override
  public @NonNull AuthenticationResult tryAuthenticate(
    @NonNull HttpContext context,
    @NonNull RestUserManagement management,
    @NonNull Set<String> requiredScopes
  ) {
    // check if the authorization header is present
    var authHeader = context.request().headers().firstValue(HttpHeaders.AUTHORIZATION);
    if (authHeader == null) {
      return AuthenticationResult.Constant.PROCEED;
    }

    // check if the authorization header is a bearer token
    var tokenMatcher = BEARER_LOGIN_PATTERN.matcher(authHeader);
    if (!tokenMatcher.matches()) {
      return AuthenticationResult.Constant.PROCEED;
    }

    try {
      // parse the JWT token - this call throws in case the jwt is invalid in any form
      var token = this.jwtParser.parseSignedClaims(tokenMatcher.group(1));

      // validate the token subject
      var subject = token.getPayload().getSubject();
      var user = management.restUser(UUID.fromString(subject));
      if (user == null) {
        return AuthenticationResult.Constant.USER_NOT_FOUND;
      }

      // validate that the id of the token still has access granted
      var tokenPairs = user.properties().get(JWT_TOKEN_PAIR_KEY);
      var parsedTokens = JwtTokenPropertyParser.parseTokens(tokenPairs);
      if (this.checkValidTokenId(parsedTokens, token.getPayload().getId())) {
        // the token id is registered for the user - last check we need to do is the token type checking
        var tokenId = token.getPayload().getId();
        var tokenType = token.getPayload().get("type", String.class);

        // extract the scopes from the jwt and convert it to a set for the wrapped user
        //noinspection unchecked
        var existingScopes = (List<String>) token.getPayload().getOrDefault("scopes", List.of());
        var scopesCopy = Set.copyOf(existingScopes);

        // we wrap the user to ensure that our checking later on always takes the scopes from the jwt into account
        var scopedUser = new ScopedRestUserDelegate(user, scopesCopy);
        if (!scopedUser.hasOneScopeOf(requiredScopes)) {
          return AuthenticationResult.Constant.MISSING_REQUIRED_SCOPES;
        }

        if (tokenType != null && tokenType.equals(JwtTokenHolder.ACCESS_TOKEN_TYPE)) {
          return new AuthenticationResult.Success(scopedUser, tokenId);
        } else {
          return new AuthenticationResult.InvalidTokenType(user, scopesCopy, tokenId, tokenType);
        }
      } else {
        return AuthenticationResult.Constant.INVALID_CREDENTIALS;
      }
    } catch (JwtException | JsonSyntaxException | IllegalArgumentException exception) {
      return AuthenticationResult.Constant.INVALID_CREDENTIALS;
    }
  }

  @Override
  public @NonNull AuthTokenGenerationResult generateAuthToken(
    @NonNull RestUserManagement management,
    @NonNull RestUser restUser,
    @NonNull Set<String> scopes
  ) {
    for (var scope : scopes) {
      if (!restUser.hasScope(scope)) {
        // the user requested a jwt token with scopes the user is not allowed to use
        return AuthTokenGenerationResult.Constant.REQUESTED_INVALID_SCOPES;
      }
    }

    // generate a new access and refresh token for the user
    var tokenPairId = this.newRandomTokenId();
    var accessToken = this.generateNewJwtToken(
      tokenPairId,
      JwtTokenHolder.ACCESS_TOKEN_TYPE,
      restUser,
      scopes,
      this.accessDuration);
    var refreshToken = this.generateNewJwtToken(
      tokenPairId,
      JwtTokenHolder.REFRESH_TOKEN_TYPE,
      restUser,
      scopes,
      this.refreshDuration);

    // get the tokens that are currently stored in the rest user
    var tokenProperty = restUser.properties().get(JWT_TOKEN_PAIR_KEY);
    var parsedStoredTokens = JwtTokenPropertyParser.parseTokens(tokenProperty);

    // remove the outdated tokens and register the new ones
    var currentTime = Instant.now();
    var tokens = parsedStoredTokens.stream()
      .filter(holder -> currentTime.isBefore(holder.expiresAt()))
      .collect(Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), list -> {
        list.add(accessToken);
        list.add(refreshToken);
        return list;
      }));

    // update the token list
    var compactedTokens = JwtTokenPropertyParser.compactTokens(tokens);
    var updatedUser = management.builder(restUser).property(JWT_TOKEN_PAIR_KEY, compactedTokens).build();
    management.saveRestUser(updatedUser);

    // return the generated token pair
    return new AuthTokenGenerationResult.Success<>(new JwtAuthToken(
      scopes.isEmpty() ? updatedUser.scopes() : scopes,
      currentTime,
      accessToken,
      refreshToken));
  }

  private @NonNull String newRandomTokenId() {
    return UUID.randomUUID().toString();
  }

  private @NonNull JwtTokenHolder generateNewJwtToken(
    @NonNull String tokenId,
    @NonNull String tokenType,
    @NonNull RestUser subject,
    @NonNull Set<String> scopes,
    @NonNull Duration validDuration
  ) {
    var expiration = Instant.now().plus(validDuration);
    var jwtTokenBuilder = Jwts.builder()
      .issuer(this.issuer)
      .subject(subject.id().toString())
      .issuedAt(new Date())
      .expiration(Date.from(expiration))
      .id(tokenId)
      .claim("type", tokenType)
      .signWith(this.jwtSigningKey);
    // only add the scopes if the user explicitly requested to do so
    if (!scopes.isEmpty()) {
      jwtTokenBuilder = jwtTokenBuilder.claim("scopes", scopes);
    }

    return new JwtTokenHolder(jwtTokenBuilder.compact(), tokenId, expiration, tokenType);
  }

  protected boolean checkValidTokenId(@NonNull Collection<JwtTokenHolder> tokens, @NonNull String tokenId) {
    var currentTime = Instant.now();
    return tokens.stream()
      .filter(holder -> currentTime.isBefore(holder.expiresAt()))
      .anyMatch(holder -> holder.tokenId().equals(tokenId));
  }
}
