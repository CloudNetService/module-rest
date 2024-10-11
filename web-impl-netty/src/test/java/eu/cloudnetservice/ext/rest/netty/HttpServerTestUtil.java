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

package eu.cloudnetservice.ext.rest.netty;

import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public final class HttpServerTestUtil {

  private HttpServerTestUtil() {
    throw new UnsupportedOperationException();
  }

  public static HostAndPort resolveFreeHost() {
    try (var socket = new ServerSocket()) {
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress("127.0.0.1", 0));
      return new HostAndPort("127.0.0.1", socket.getLocalPort());
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to find free port to bind to", exception);
    }
  }
}
