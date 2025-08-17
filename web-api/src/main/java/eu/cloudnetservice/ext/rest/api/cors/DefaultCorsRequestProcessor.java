/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.ext.rest.api.cors;

import com.google.common.base.Splitter;
import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.config.CorsConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.connection.BasicHttpConnectionInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of the cors request processor.
 *
 * @see CorsRequestProcessor
 * @since 1.0
 */
public final class DefaultCorsRequestProcessor implements CorsRequestProcessor {

  private static final String ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK = "Access-Control-Request-Private-Network";
  private static final Splitter REQUEST_HEADERS_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable CorsPreflightRequestInfo extractInfoFromPreflightRequest(@NonNull HttpRequest request) {
    // check if the request method is the expected
    var requestMethod = request.method();
    if (!requestMethod.equalsIgnoreCase(HttpMethod.OPTIONS.name())) {
      return null;
    }

    // check if the required headers origin & request method are present
    var origin = request.headers().firstValue(HttpHeaders.ORIGIN);
    var clientRequestMethod = request.headers().firstValue(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    if (origin == null || clientRequestMethod == null) {
      return null;
    }

    // extract the client request headers, default to empty list if not given
    var clientRequestHeaders = request.headers().firstValue(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    if (clientRequestHeaders == null) {
      return new CorsPreflightRequestInfo(origin, clientRequestMethod, List.of());
    }

    return new CorsPreflightRequestInfo(
      origin,
      clientRequestMethod,
      REQUEST_HEADERS_SPLITTER.splitToList(clientRequestHeaders));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processPreflightRequest(
    @NonNull HttpContext context,
    @NonNull CorsPreflightRequestInfo preflightRequestInfo,
    @Nullable HttpHandlerConfig httpHandlerConfig
  ) {
    // check if the request needs CORS information
    var crossOrigin = this.extractCrossOrigin(context.request(), context.connectionInfo());
    if (crossOrigin == null) {
      return;
    }

    // ensure that a corsConfig configuration is present in order to process the CORS request
    var corsConfig = httpHandlerConfig != null ? httpHandlerConfig.corsConfig() : null;
    if (corsConfig == null) {
      this.rejectRequest(context.response());
      return;
    }

    this.filterAndPreprocessCorsRequest(
      preflightRequestInfo.origin(),
      preflightRequestInfo.requestMethod(),
      httpHandlerConfig.httpMethod(),
      preflightRequestInfo.requestHeaders(),
      corsConfig,
      context.request(),
      context.response(),
      true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean processNormalRequest(@NonNull HttpContext context, @NonNull HttpHandlerConfig httpHandlerConfig) {
    // check if the request needs CORS information
    var crossOrigin = this.extractCrossOrigin(context.request(), context.connectionInfo());
    if (crossOrigin == null) {
      return true;
    }

    // check if a cors config is present for the handler
    var corsConfig = httpHandlerConfig.corsConfig();
    if (corsConfig == null) {
      return true;
    }

    // get the base information needed to handle the CORS request
    var request = context.request();
    var headerNames = context.request().headers().names();

    // actually handle and validate the request
    return this.filterAndPreprocessCorsRequest(
      crossOrigin,
      request.method(),
      httpHandlerConfig.httpMethod(),
      headerNames,
      corsConfig,
      request,
      context.response(),
      false);
  }

  private boolean filterAndPreprocessCorsRequest(
    @NonNull String origin,
    @NonNull String method,
    @NonNull HttpMethod allowedMethod,
    @NonNull Collection<String> headerNames,
    @NonNull CorsConfig corsConfig,
    @NonNull HttpRequest httpRequest,
    @NonNull HttpResponse httpResponse,
    boolean preflight
  ) {
    // append information about the headers that on change might lead to a different handling result
    httpResponse.headers().add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    httpResponse.headers().add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    httpResponse.headers().add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

    // check if the given origin is valid
    var validOrigin = corsConfig.findMatchingOrigin(origin);
    if (validOrigin == null) {
      this.rejectRequest(httpResponse);
      return false;
    }

    // check if the used request method is valid
    if (!method.equalsIgnoreCase(allowedMethod.name())) {
      this.rejectRequest(httpResponse);
      return false;
    }

    if (preflight) {
      // validate that the supplied headers are allowed
      var filteredHeaders = corsConfig.filterHeaders(headerNames);
      if (filteredHeaders == null) {
        this.rejectRequest(httpResponse);
        return false;
      }

      // respond with the allowed filtered headers
      if (!filteredHeaders.isEmpty()) {
        var allowedHeaderString = String.join(", ", filteredHeaders);
        httpResponse.headers().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaderString);
      }

      // add info about the allowed method for the request
      httpResponse.headers().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethod.name());

      // add info about the max time the preflight response can be cached, if provided
      var maxAge = corsConfig.maxAge();
      if (maxAge != -1) {
        httpResponse.headers().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
      }

      // indicates to the browser is requests from public networks to private networks are allowed
      // https://developer.chrome.com/blog/private-network-access-preflight
      if (httpRequest.headers().contains(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK)) {
        var privateNetworkAllowed = Boolean.TRUE.equals(corsConfig.allowPrivateNetworks());
        httpResponse.headers().add(
          HttpHeaders.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK,
          Boolean.toString(privateNetworkAllowed));
      }

      // preflight requests are answered with 204 (no content)
      httpResponse.status(HttpResponseCode.NO_CONTENT);
    }

    // set the origin that we filtered out to be allowed
    httpResponse.headers().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, validOrigin);

    // add the exposed headers to the response in case there are any
    var exposedHeaders = corsConfig.exposedHeaders();
    if (!exposedHeaders.isEmpty()) {
      var exposedHeadersString = String.join(", ", exposedHeaders);
      httpResponse.headers().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeadersString);
    }

    // mark credentials to be allowed if configured
    if (Boolean.TRUE.equals(corsConfig.allowCredentials())) {
      httpResponse.headers().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    return true;
  }

  private void rejectRequest(@NonNull HttpResponse response) {
    response.status(HttpResponseCode.FORBIDDEN);
    response.body("Provided information does not match CORS rules");
  }

  private @Nullable String extractCrossOrigin(
    @NonNull HttpRequest request,
    @NonNull BasicHttpConnectionInfo connectionInfo
  ) {
    // check if the origin header is present
    var origin = request.headers().firstValue(HttpHeaders.ORIGIN);
    if (origin == null) {
      return null;
    }

    // extract the origin (request) information
    var parsedOrigin = URI.create(origin);
    var originScheme = parsedOrigin.getScheme();
    var originHost = parsedOrigin.getHost();
    var originPort = this.portOrDefault(originScheme, parsedOrigin.getPort());

    // get the service information
    var serverScheme = connectionInfo.scheme();
    var serverHost = connectionInfo.hostAddress().host();
    var serverPort = connectionInfo.hostAddress().port();

    if (Objects.equals(originScheme, serverScheme)
      && Objects.equals(originHost, serverHost)
      && originPort == serverPort) {
      // request is from the same origin, no CORS needed
      return null;
    } else {
      // request is from a different origin
      return origin;
    }
  }

  private int portOrDefault(@NonNull String scheme, int port) {
    if (port == -1) {
      if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("ws")) {
        port = 80;
      } else {
        port = 443;
      }
    }
    return port;
  }
}
