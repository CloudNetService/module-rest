/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.modules.rest.auth.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import lombok.NonNull;

public final class JwtSecurityUtil {

  private static final String KEY_ALGORITHM = "RSASSA-PSS";

  private JwtSecurityUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull KeyPair generateRsaPssKeyPair() {
    try {
      // RSASSA-PSS using SHA-512 and MGF1 with SHA-512
      var pssParameterSpec = new PSSParameterSpec(
        "SHA-512",
        "MGF1",
        MGF1ParameterSpec.SHA512,
        64,
        PSSParameterSpec.TRAILER_FIELD_BC);
      var rsaKeyGenParameterSpec = new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4, pssParameterSpec);

      var factory = KeyPairGenerator.getInstance(KEY_ALGORITHM);
      factory.initialize(rsaKeyGenParameterSpec);
      return factory.generateKeyPair();
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException exception) {
      throw new IllegalStateException("Unable to generate RsaSsa-Pss-Sha-512 JWT singing keys", exception);
    }
  }

  public static @NonNull KeyPair pairFromEncodedKeys(byte[] encodedPublic, byte[] encodedPrivate) {
    try {
      var keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
      var decodedPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublic));
      var decodedPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivate));
      return new KeyPair(decodedPublicKey, decodedPrivateKey);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
      throw new IllegalStateException("Unable to decode RsaSsa-Pss-Sha-512 JWT singing keys", exception);
    }
  }
}
