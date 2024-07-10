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

package eu.cloudnetservice.ext.rest.api.auth.ticket;

import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebSocketTicketProviderLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthProviderLoader.class);

  private static final Map<String, WebSocketTicketProvider<?>> WEB_SOCKET_TICKET_PROVIDERS;

  static {
    WEB_SOCKET_TICKET_PROVIDERS = ServiceLoader.load(
        WebSocketTicketProvider.class,
        WebSocketTicketProvider.class.getClassLoader())
      .stream()
      .map(provider -> {
        try {
          return provider.get();
        } catch (ServiceConfigurationError error) {
          LOGGER.debug(
            "Error creating instance of websocket ticket provider impl: {}",
            provider.type().getSimpleName(),
            error);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .sorted(Comparator.comparingInt(WebSocketTicketProvider::priority))
      .collect(Collectors.toMap(
        ticketProvider -> ticketProvider.name().toLowerCase(Locale.ROOT),
        value -> (WebSocketTicketProvider<?>) value,
        (left, __) -> left));
  }

  private WebSocketTicketProviderLoader() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull WebSocketTicketProvider<?> resolveWebSocketTicketProvider(@NonNull String name) {
    var ticketProvider = WEB_SOCKET_TICKET_PROVIDERS.get(name.toLowerCase(Locale.ROOT));
    if (ticketProvider == null) {
      throw new IllegalArgumentException("No websocket ticket provider registered with name: " + name);
    }

    return ticketProvider;
  }
}
