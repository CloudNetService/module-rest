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

import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NettyHttpServerTest {

  @Test
  void testRejectsRequestWithBadChar() throws Exception {
    var bindHost = HttpServerTestUtil.resolveFreeHost();
    var config = ComponentConfig.builder().executorService(Executors.newSingleThreadExecutor()).build();
    var server = new NettyHttpServer(config);
    server.addListener(bindHost).join();

    var socket = new Socket();
    socket.setReuseAddress(true);
    socket.connect(new InetSocketAddress(bindHost.host(), bindHost.port()));

    var out = socket.getOutputStream();
    out.write(
      """
        GET / HTTP/1.1
        Test: Hello\u0001World
        Content-Type: application/json
        Content-Length: 0
        
        """.getBytes(StandardCharsets.UTF_8));
    out.flush();

    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    var lines = reader.lines().toList();
    Assertions.assertEquals("HTTP/1.1 400 Bad Request", lines.getFirst());
    Assertions.assertTrue(lines.contains("content-length: 0"));
    Assertions.assertTrue(lines.contains("connection: close"));
    Assertions.assertNull(reader.readLine()); // server should've closed the connection now
  }
}
