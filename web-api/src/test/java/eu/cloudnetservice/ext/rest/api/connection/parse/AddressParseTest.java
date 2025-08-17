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

import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;

public final class AddressParseTest {

  @Test
  public void testIpv4() throws Throwable {
    var dummyHeader = "Host";
    var defaultPort = 2023;

    ThrowingSupplier<HostAndPort> withPort = () -> AddressParseUtil.parseHostAndPort(
      dummyHeader,
      "127.0.0.1:1870",
      defaultPort);
    Assertions.assertDoesNotThrow(withPort);
    Assertions.assertEquals(1870, withPort.get().port());
    Assertions.assertEquals("127.0.0.1", withPort.get().host());

    ThrowingSupplier<HostAndPort> noPort = () -> AddressParseUtil.parseHostAndPort(
      dummyHeader,
      "127.0.0.1",
      defaultPort);
    Assertions.assertDoesNotThrow(noPort);
    Assertions.assertEquals(defaultPort, noPort.get().port());
    Assertions.assertEquals("127.0.0.1", noPort.get().host());

    Executable invalidPort = () -> AddressParseUtil.parseHostAndPort(
      dummyHeader,
      "127.0.0.1:16082023",
      defaultPort);
    Assertions.assertThrows(IllegalArgumentException.class, invalidPort);
  }

  @Test
  public void testIpv6() throws Throwable {
    var dummyHeader = "Host";
    var defaultPort = 2023;

    ThrowingSupplier<HostAndPort> withPort = () -> AddressParseUtil.parseHostAndPort(
      dummyHeader,
      "[3610::]:1870",
      defaultPort);
    Assertions.assertDoesNotThrow(withPort);
    Assertions.assertEquals(1870, withPort.get().port());
    Assertions.assertEquals("3610::", withPort.get().host());

    ThrowingSupplier<HostAndPort> noPort = () -> AddressParseUtil.parseHostAndPort(
      dummyHeader,
      "[3610::]",
      defaultPort);
    Assertions.assertDoesNotThrow(noPort);
    Assertions.assertEquals(defaultPort, noPort.get().port());
    Assertions.assertEquals("3610::", noPort.get().host());

    Executable invalidPort = () -> AddressParseUtil.parseHostAndPort(
      dummyHeader,
      "[3610::]:16082023",
      defaultPort);
    Assertions.assertThrows(IllegalArgumentException.class, invalidPort);
  }

}
