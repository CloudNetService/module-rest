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

package eu.cloudnetservice.ext.rest.api.annotation.parser.processor;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.problem.ProblemHttpHandleException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class AuthenticationProcessor implements HttpAnnotationProcessor {

  private static final ProblemDetail AUTH_METHOD_UNKNOWN = ProblemDetail.builder()
    .title("Auth Method Unknown")
    .status(HttpResponseCode.BAD_REQUEST)
    .type(URI.create("auth-method-unknown"))
    .detail("Requested authentication method is unavailable")
    .build();

  private static final ProblemDetail AUTH_INVALID = ProblemDetail.builder()
    .title("Auth Invalid")
    .type(URI.create("auth-invalid"))
    .status(HttpResponseCode.UNAUTHORIZED)
    .detail("The provided auth information is invalid")
    .build();

  private static final ProblemDetail AUTH_REQUIRED_SCOPE_MISSING = ProblemDetail.builder()
    .title("Auth Required Scope Missing")
    .type(URI.create("auth-required-scope-missing"))
    .status(HttpResponseCode.FORBIDDEN)
    .detail("The authenticated user misses a required scope to access the resource")
    .build();

  private final Supplier<RestUserManagement> management;

  public AuthenticationProcessor(@NonNull Supplier<RestUserManagement> management) {
    this.management = management;
  }

  private static @Nullable Authentication extractAuthAnnotation(@NonNull Method method) {
    if (method.isAnnotationPresent(Authentication.class)) {
      return method.getAnnotation(Authentication.class);
    }

    var declaringClass = method.getDeclaringClass();
    if (declaringClass.isAnnotationPresent(Authentication.class)) {
      return declaringClass.getAnnotation(Authentication.class);
    }

    return null;
  }

  private static @NonNull List<? extends AuthProvider> resolveProviders(@NonNull Authentication authentication) {
    var providers = Arrays.stream(authentication.providers()).map(AuthProviderLoader::resolveAuthProvider).toList();
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("No auth providers given in @Authentication annotation");
    }

    return providers;
  }

  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handlerInstance
  ) {
    var hints = HttpAnnotationProcessorUtil.mapParameters(
      method,
      Authentication.class,
      (param, annotation) -> {
        var scopes = Set.of(annotation.scopes());
        var provider = resolveProviders(annotation);
        return context -> this.tryAuthenticateRequest(context, provider, scopes);
      });

    // the auth annotation should only be at one parameter, there is no point in supplying it multiple times
    if (hints.size() > 1) {
      throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
        .handlerMethod(method)
        .annotationType(Authentication.class)
        .debugDescription("The auth annotation should not be present on more than one parameter")
        .build();
    }

    // only check for the annotation on the class level in case the annotation is not present on a parameter
    if (hints.isEmpty()) {
      var authentication = extractAuthAnnotation(method);
      var provider = authentication != null ? resolveProviders(authentication) : null;
      if (provider != null) {
        // there were declared auth providers, register a pre-processor to handle authentication
        var scopes = Set.of(authentication.scopes());
        config.addHandlerInterceptor(new HttpHandlerInterceptor() {
          @Override
          public boolean preProcess(
            @NonNull HttpContext context,
            @NonNull HttpHandler handler,
            @NonNull HttpHandlerConfig config
          ) {
            AuthenticationProcessor.this.tryAuthenticateRequest(context, provider, scopes);
            return true;
          }
        });
      }
    } else {
      // there might be an authentication annotation present on one parameter, register our hints
      config.addHandlerInterceptor(new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      });
    }
  }

  private @NonNull RestUser tryAuthenticateRequest(
    @NonNull HttpContext context,
    @NonNull List<? extends AuthProvider> provider,
    @NonNull Set<String> scopes
  ) {
    // try all requested auth providers until we find one that can handle the authentication process
    AuthenticationResult authenticationResult = AuthenticationResult.Constant.PROCEED;
    for (var authProvider : provider) {
      authenticationResult = authProvider.tryAuthenticate(context, this.management.get(), scopes);
      if (authenticationResult != AuthenticationResult.Constant.PROCEED) {
        break;
      }
    }

    return switch (authenticationResult) {
      case AuthenticationResult.Success success -> success.restUser();
      case AuthenticationResult.Constant.PROCEED -> throw new ProblemHttpHandleException(AUTH_METHOD_UNKNOWN);
      case AuthenticationResult.Constant.MISSING_REQUIRED_SCOPES ->
        throw new ProblemHttpHandleException(AUTH_REQUIRED_SCOPE_MISSING);
      default -> throw new ProblemHttpHandleException(AUTH_INVALID);
    };
  }
}
