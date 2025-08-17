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

package eu.cloudnetservice.ext.rest.api.connection.parse;

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HeaderMockUtil;
import eu.cloudnetservice.ext.rest.api.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ForwardedHeaderTest {

  private static final HostAndPort EMPTY_HOST = new HostAndPort("INVALID", -1);
  private static final BasicHttpConnectionInfo EMPTY_INFO = new BasicHttpConnectionInfo(
    "INVALID",
    EMPTY_HOST,
    EMPTY_HOST);

  @Test
  public void testForwardedHeader() {
    var resolver = new ForwardedSyntaxConnectionInfoResolver(HttpHeaders.FORWARDED);
    var context = HeaderMockUtil.setupContext(HttpHeaderMap.newHeaderMap()
      .add(HttpHeaders.FORWARDED, "for=192.0.2.60;proto=http;host=203.0.113.43"));

    var info = resolver.extractConnectionInfo(context, EMPTY_INFO);

    Assertions.assertEquals("http", info.scheme());
    Assertions.assertEquals(new HostAndPort("203.0.113.43", 80), info.hostAddress());
    Assertions.assertEquals(new HostAndPort("192.0.2.60", -1), info.clientAddress());
  }
}
