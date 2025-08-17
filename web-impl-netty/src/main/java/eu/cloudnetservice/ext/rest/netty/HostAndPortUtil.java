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

package eu.cloudnetservice.ext.rest.netty;

import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import io.netty5.channel.socket.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

final class HostAndPortUtil {

  private HostAndPortUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Tries to convert the given socket address into a host and port, throwing an exception if not possible.
   *
   * @param socketAddress the socket address to convert.
   * @return the created host and port based on the given address.
   * @throws NullPointerException     if the given socket address is null.
   * @throws IllegalArgumentException if the given socket address type cannot be converted.
   */
  @Contract("_ -> new")
  public static @NonNull HostAndPort extractFromSocketAddressInfo(@NonNull SocketAddress socketAddress) {
    // inet socket address
    if (socketAddress instanceof InetSocketAddress inet) {
      return new HostAndPort(inet.getAddress().getHostAddress(), inet.getPort());
    }

    // unix socket address
    if (socketAddress instanceof UnixDomainSocketAddress unixSocketAddress) {
      return new HostAndPort(unixSocketAddress.getPath().toString(), HostAndPort.NO_PORT);
    }

    // unix socket address but from netty
    if (socketAddress instanceof DomainSocketAddress domainSocketAddress) {
      return new HostAndPort(domainSocketAddress.path(), HostAndPort.NO_PORT);
    }

    // unsupported
    throw new IllegalArgumentException("Unsupported socket address type: " + socketAddress.getClass().getName());
  }
}
