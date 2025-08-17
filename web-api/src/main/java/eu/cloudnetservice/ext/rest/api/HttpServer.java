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

package eu.cloudnetservice.ext.rest.api;

import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

/**
 * Represents a http component which can be bound, receive and handle http messages.
 *
 * @since 1.0
 */
public interface HttpServer extends HttpComponent<HttpServer> {

  /**
   * Binds this http server to the given port on any local address (ipv6 form: {@code ::/0}) if no other listener is
   * already listening to that port.
   *
   * @param port the port to which the listener should get bound.
   * @return a future completed exceptionally if the bind fails, normally if the bind succeeded.
   * @throws IllegalArgumentException if the given port exceeds the port range.
   */
  @NonNull CompletableFuture<Void> addListener(int port);

  /**
   * Binds this network server to the given host and port if no listener is already listening on the given address.
   *
   * @param hostAndPort the address to which a listener should get bound.
   * @return a future completed exceptionally if the bind fails, normally if the bind succeeded.
   * @throws NullPointerException if the given host and port is null.
   */
  @NonNull CompletableFuture<Void> addListener(@NonNull HostAndPort hostAndPort);
}
