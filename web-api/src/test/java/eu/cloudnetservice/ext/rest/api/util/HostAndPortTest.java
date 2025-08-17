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

package eu.cloudnetservice.ext.rest.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class HostAndPortTest {

  @Test
  public void testInvalidPorts() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new HostAndPort("127.0.0.1", 187361));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new HostAndPort("0.0.0.0", -420));
  }

  @Test
  public void testToString() {
    Assertions.assertEquals("localhost:187", new HostAndPort("localhost", 187).toString());
  }

  @Test
  public void testPortValidation() {
    Assertions.assertFalse(new HostAndPort("localhost", -1).validPort());
    Assertions.assertTrue(new HostAndPort("187.187.187.187", 26).validPort());
  }

}
