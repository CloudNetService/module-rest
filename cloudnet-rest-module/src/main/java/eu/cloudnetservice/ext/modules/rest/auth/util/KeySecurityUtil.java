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

package eu.cloudnetservice.ext.modules.rest.auth.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
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
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import lombok.NonNull;

public final class KeySecurityUtil {

  public static final String HMAC_SHA_256_ALGORITHM_NAME = "HmacSHA256";
  private static final String RSA_KEY_ALGORITHM_NAME = "RSASSA-PSS";

  private KeySecurityUtil() {
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

      var factory = KeyPairGenerator.getInstance(RSA_KEY_ALGORITHM_NAME);
      factory.initialize(rsaKeyGenParameterSpec);
      return factory.generateKeyPair();
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException exception) {
      throw new IllegalStateException("Unable to generate RsaSsa-Pss-Sha-512 JWT singing keys", exception);
    }
  }

  public static @NonNull KeyPair pairFromEncodedKeys(byte[] encodedPublic, byte[] encodedPrivate) {
    try {
      var keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM_NAME);
      var decodedPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublic));
      var decodedPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivate));
      return new KeyPair(decodedPublicKey, decodedPrivateKey);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
      throw new IllegalStateException("Unable to decode RsaSsa-Pss-Sha-512 JWT singing keys", exception);
    }
  }

  public static @NonNull Key generateHmacSHA256Key() {
    try {
      var keyGenerator = KeyGenerator.getInstance(HMAC_SHA_256_ALGORITHM_NAME);
      return keyGenerator.generateKey();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to generate HmacSHA256 ticket signing key", exception);
    }
  }

  public static @NonNull Key hmacSHA256KeyFromEncoded(byte[] encodedKey) {
    return new SecretKeySpec(encodedKey, HMAC_SHA_256_ALGORITHM_NAME);
  }
}
