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

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cors request processor capable of extracting cors information, handling preflight requests and ensuring that every
 * request meets the cors requirements.
 *
 * @see eu.cloudnetservice.ext.rest.api.config.CorsConfig
 * @since 1.0
 */
public interface CorsRequestProcessor {

  /**
   * Extracts the origin, the used method and the requested headers from the given request. A successful extraction
   * indicates that the request is a cors preflight request, null is returned otherwise.
   * <p>
   * The requirements for a cors preflight request are:
   * <ul>
   *   <li>The used http method is {@link eu.cloudnetservice.ext.rest.api.HttpMethod#OPTIONS}</li>
   *   <li>The client {@link com.google.common.net.HttpHeaders#ORIGIN} header is set</li>
   *   <li>The {@link com.google.common.net.HttpHeaders#ACCESS_CONTROL_REQUEST_METHOD} header is set</li>
   * </ul>
   *
   * @param request the request to extract the cors information from.
   * @return the extracted cors preflight request info, null if the request is not a preflight request.
   * @throws NullPointerException if the given request is null.
   */
  @Nullable CorsPreflightRequestInfo extractInfoFromPreflightRequest(@NonNull HttpRequest request);

  /**
   * Processes the incoming preflight request while making sure that CORS really is needed for the request and CORS is
   * enabled in the given handler config.
   *
   * @param context              the context of the incoming request.
   * @param preflightRequestInfo the preflight data for the request.
   * @param httpHandlerConfig    the config of the handler handling the request.
   * @throws NullPointerException if the given context or request info is null.
   */
  void processPreflightRequest(
    @NonNull HttpContext context,
    @NonNull CorsPreflightRequestInfo preflightRequestInfo,
    @Nullable HttpHandlerConfig httpHandlerConfig);

  /**
   * Gets whether the request can be processed normally or if the request is rejected due to CORS related problems.
   * <p>
   * If the request is a CORS request and everything passes, CORS related headers are added by this method.
   *
   * @param context           the context of the incoming request.
   * @param httpHandlerConfig the config of the handler handling the request.
   * @return true if the request should be processed, false otherwise.
   * @throws NullPointerException if the context or the handler config is null.
   */
  boolean processNormalRequest(@NonNull HttpContext context, @NonNull HttpHandlerConfig httpHandlerConfig);
}
