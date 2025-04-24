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

import io.vavr.Tuple2;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lombok.NonNull;

public final class PasswordEncryptionUtil {

  private static final int KEY_LENGTH = 512;
  private static final int ITERATION_COUNT = 500_000;

  private static final SecureRandom SALT_GENERATION_RANDOM = new SecureRandom();

  private PasswordEncryptionUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Tuple2<String, String> encrypt(@NonNull String password) {
    // generate a new salt and encrypt the password with it
    var salt = newRandomSalt();
    var passwordHex = encrypt(salt, password);

    // convert the generated salt to hex and return the salt and password info
    var saltHex = HexFormat.of().formatHex(salt);
    return new Tuple2<>(saltHex, passwordHex);
  }

  public static @NonNull String encrypt(@NonNull String saltHex, @NonNull String password) {
    // encrypt the given password as hex using the salt decoded from the given salt hex
    var salt = HexFormat.of().parseHex(saltHex);
    return encrypt(salt, password);
  }

  private static @NonNull String encrypt(byte[] salt, @NonNull String password) {
    try {
      // hashes the password using the PBKDF2 with SHA256 as PRF
      var keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
      var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
      var hash = factory.generateSecret(keySpec).getEncoded();

      // return the generated password as hex
      return HexFormat.of().formatHex(hash);
    } catch (InvalidKeySpecException e) {
      throw new IllegalStateException("Unable to generate secret from key spec");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to retrieve PBKDF2WithHmacSHA256 key factory");
    }
  }

  private static byte[] newRandomSalt() {
    // see https://datatracker.ietf.org/doc/html/rfc8018#section-4.1
    var bytes = new byte[64];
    SALT_GENERATION_RANDOM.nextBytes(bytes);
    return bytes;
  }
}
