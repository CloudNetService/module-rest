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

package eu.cloudnetservice.ext.rest.ticket;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public final class TicketSecurityUtilTest {

  private static Mac hashFunction;

  @BeforeAll
  static void setup() throws NoSuchAlgorithmException, InvalidKeyException {
    var mac = Mac.getInstance("HmacSHA256");
    var keyGen = KeyGenerator.getInstance("HmacSHA256");
    mac.init(keyGen.generateKey());

    hashFunction = mac;
  }

  @Test
  void testSuccessfulTicketSign() {
    var data = " test ticket data super secret ";
    var signedTicket = TicketSecurityUtil.signTicket(hashFunction, data);

    Assertions.assertNotNull(signedTicket);
    // the result should not contain white space
    Assertions.assertEquals(signedTicket, signedTicket.strip());
    Assertions.assertEquals(data, TicketSecurityUtil.extractTicketInformation(signedTicket));
    Assertions.assertTrue(TicketSecurityUtil.verifyTicketSignature(hashFunction, signedTicket));
  }

  @Test
  void testIllegalTicketFormat() {
    var data = "superTestData";
    var signedTicket = TicketSecurityUtil.signTicket(hashFunction, data).replace('.', ':');

    Assertions.assertNull(TicketSecurityUtil.extractTicketInformation(signedTicket));
    Assertions.assertFalse(TicketSecurityUtil.verifyTicketSignature(hashFunction, signedTicket));
  }

  @Test
  void testModifiedTicketData() {
    var data = "superTestData";
    var signedTicket = TicketSecurityUtil.signTicket(hashFunction, data);
    var parts = signedTicket.split("\\.", 2);
    var hash = parts[1];

    Assertions.assertFalse(TicketSecurityUtil.verifyTicketSignature(hashFunction, "superTestDataFaked." + hash));
  }
}
