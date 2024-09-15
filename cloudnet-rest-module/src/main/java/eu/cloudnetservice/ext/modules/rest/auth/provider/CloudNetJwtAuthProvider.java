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
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import lombok.NonNull;

public final class CloudNetJwtAuthProvider extends JwtAuthProvider {

  private static final Path PRIVATE_KEY_PATH = Path.of("jwt_sign_key");
  private static final Path PUBLIC_KEY_PATH = Path.of("jwt_sign_key.pub");

  public CloudNetJwtAuthProvider() {
    var authConfig = RestConfiguration.get().authConfig();
    super(
      "CloudNet Rest",
      readOrGenerateJwtKeyPair(),
      authConfig.jwtTokenLifetime(),
      authConfig.jwtRefreshTokenLifetime());
  }

  private static @NonNull KeyPair readOrGenerateJwtKeyPair() {
    try {
      // hack: due to this class being constructed via SPI, we use injection layer here
      // resolving works via the class loader, but we ensure anyway that we did not get the ext layer as fallback
      // it must be the module layer in order to provide the correct instance to get the module data directory
      var moduleInjectLayer = InjectionLayer.findLayerOf(CloudNetJwtAuthProvider.class.getClassLoader());
      Preconditions.checkState(moduleInjectLayer != InjectionLayer.ext(), "Cannot resolve module injection layer");

      // resolve the path where the private and public key for jwt singing should be located
      var moduleDataDir = moduleInjectLayer.instance(CloudNetRestModule.class).moduleWrapper().dataDirectory();
      var publicKeyPath = moduleDataDir.resolve(PUBLIC_KEY_PATH);
      var privateKeyPath = moduleDataDir.resolve(PRIVATE_KEY_PATH);

      // if one of the keys is missing the key pair is incomplete - generate new keys in that case
      if (Files.notExists(publicKeyPath) || Files.notExists(privateKeyPath)) {
        var jwtSignKeyPair = KeySecurityUtil.generateRsaPssKeyPair();

        Files.createDirectories(moduleDataDir);
        Files.write(publicKeyPath, jwtSignKeyPair.getPublic().getEncoded());
        Files.write(privateKeyPath, jwtSignKeyPair.getPrivate().getEncoded());
      }

      // read and decode the jwt signing key pair
      var encodedPublicKey = Files.readAllBytes(publicKeyPath);
      var encodedPrivateKey = Files.readAllBytes(privateKeyPath);
      return KeySecurityUtil.pairFromEncodedKeys(encodedPublicKey, encodedPrivateKey);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to initialize JWT signing key pair", exception);
    }
  }

  @Override
  public int priority() {
    return AuthProvider.DEFAULT_PRIORITY + 10;
  }
}
