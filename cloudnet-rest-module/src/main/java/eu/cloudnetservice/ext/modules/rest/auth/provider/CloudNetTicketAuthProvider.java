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

package eu.cloudnetservice.ext.modules.rest.auth.provider;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.modules.rest.CloudNetRestModule;
import eu.cloudnetservice.ext.modules.rest.auth.util.KeySecurityUtil;
import eu.cloudnetservice.ext.modules.rest.config.RestConfiguration;
import eu.cloudnetservice.ext.rest.ticket.TicketAuthProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import lombok.NonNull;

public final class CloudNetTicketAuthProvider extends TicketAuthProvider {

  private static final Path HMAC_KEY_PATH = Path.of("ticket_sign_key");

  public CloudNetTicketAuthProvider() {
    super(RestConfiguration.get().authConfig().ticketLifetime(), readOrGenenerateMAC());
  }

  private static @NonNull Mac readOrGenenerateMAC() {
    try {
      // hack: due to this class being constructed via SPI, we use injection layer here
      // resolving works via the class loader, but we ensure anyway that we did not get the ext layer as fallback
      // it must be the module layer in order to provide the correct instance to get the module data directory
      var moduleInjectLayer = InjectionLayer.findLayerOf(CloudNetTicketAuthProvider.class.getClassLoader());
      Preconditions.checkState(moduleInjectLayer != InjectionLayer.ext(), "Cannot resolve module injection layer");

      // resolve the path where the private and public key for jwt singing should be located
      var moduleDataDir = moduleInjectLayer.instance(CloudNetRestModule.class).moduleWrapper().dataDirectory();
      var hmacKeyPath = moduleDataDir.resolve(HMAC_KEY_PATH);

      if (Files.notExists(hmacKeyPath)) {
        var hmacSHA256Key = KeySecurityUtil.generateHmacSHA256Key();

        Files.createDirectories(moduleDataDir);
        Files.write(hmacKeyPath, hmacSHA256Key.getEncoded());
      }

      // read and decode the HmacSHA256 key for ws tickets
      var decodedHmacSHA256Key = KeySecurityUtil.hmacSHA256KeyFromEncoded(Files.readAllBytes(hmacKeyPath));
      var mac = Mac.getInstance(decodedHmacSHA256Key.getAlgorithm());
      mac.init(decodedHmacSHA256Key);
      return mac;
    } catch (IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("Unable to initialize JWT signing key pair", exception);
    }
  }
}
