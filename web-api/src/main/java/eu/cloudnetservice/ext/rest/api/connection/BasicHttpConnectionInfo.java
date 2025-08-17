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

package eu.cloudnetservice.ext.rest.api.connection;

import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import java.util.Locale;
import lombok.NonNull;

/**
 * A basic connection info holding data about the sender of a request.
 *
 * @param scheme        the scheme used when sending the request.
 * @param hostAddress   the address of the server that received the request.
 * @param clientAddress the address of the client that sent the request.
 * @since 1.0
 */
public record BasicHttpConnectionInfo(
  @NonNull String scheme,
  @NonNull HostAndPort hostAddress,
  @NonNull HostAndPort clientAddress
) {

  /**
   * A basic connection info holding data about the sender of a request.
   *
   * @param scheme        the scheme used when sending the request.
   * @param hostAddress   the address of the server that received the request.
   * @param clientAddress the address of the client that sent the request.
   * @throws NullPointerException if the given scheme, host or client is null.
   * @since 1.0
   */
  public BasicHttpConnectionInfo {
    scheme = scheme.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Creates a new basic connection info from this connection and sets the scheme to the given one.
   *
   * @param scheme the scheme to set.
   * @return a new basic connection info with the given scheme.
   * @throws NullPointerException if the given scheme is null.
   */
  public @NonNull BasicHttpConnectionInfo withScheme(@NonNull String scheme) {
    return new BasicHttpConnectionInfo(scheme, this.hostAddress, this.clientAddress);
  }

  /**
   * Creates a new basic connection info from this connection and sets the host address to the given one.
   *
   * @param hostAddress the host address to set.
   * @return a new basic connection info with the given host address.
   * @throws NullPointerException if the given host address is null.
   */
  public @NonNull BasicHttpConnectionInfo withHostAddress(@NonNull HostAndPort hostAddress) {
    return new BasicHttpConnectionInfo(this.scheme, hostAddress, this.clientAddress);
  }

  /**
   * Creates a new basic connection info from this connection and sets the client address to the given one.
   *
   * @param clientAddress the scheme to set.
   * @return a new basic connection info with the given client address.
   * @throws NullPointerException if the given client address is null.
   */
  public @NonNull BasicHttpConnectionInfo withClientAddress(@NonNull HostAndPort clientAddress) {
    return new BasicHttpConnectionInfo(this.scheme, this.hostAddress, clientAddress);
  }

  /**
   * Gets the default port that is usually used for the specified scheme. In that case {@code http} and {@code ws} will
   * return 80, everything else 443
   *
   * @return the default port that is usually used for the specified scheme.
   */
  public int defaultPortForScheme() {
    if (this.scheme.equals("http") || this.scheme.equals("ws")) {
      return 80;
    } else {
      return 443;
    }
  }
}
