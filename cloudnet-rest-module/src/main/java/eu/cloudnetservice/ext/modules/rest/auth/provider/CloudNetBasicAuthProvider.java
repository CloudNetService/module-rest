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

package eu.cloudnetservice.ext.modules.rest.auth.provider;

import eu.cloudnetservice.ext.modules.rest.auth.DefaultRestUser;
import eu.cloudnetservice.ext.modules.rest.auth.util.PasswordEncryptionUtil;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.basic.BasicAuthProvider;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

public final class CloudNetBasicAuthProvider extends BasicAuthProvider {

  @Override
  public int priority() {
    return AuthProvider.DEFAULT_PRIORITY + 10;
  }

  @Override
  public boolean validatePassword(@NonNull RestUser user, byte[] passwordBytes) {
    // get the password and salt info from the given rest user
    var storedPassword = user.properties().get(DefaultRestUser.PASSWORD_KEY);
    var storedSalt = user.properties().get(DefaultRestUser.PASSWORD_SALT_KEY);
    if (storedPassword == null || storedSalt == null) {
      return false;
    }

    // hash the given password and check if it matches the expected one
    // converting the given password bytes into a string will put it into memory until the GC clears it, unfortunately
    // there doesn't seem to be a method to pass the byte array to the encryption process directly
    var password = new String(passwordBytes, StandardCharsets.UTF_8);
    var hashedPassword = PasswordEncryptionUtil.encrypt(storedSalt, password);

    // check if the stored password is equal to the hashed password from the given input
    return storedPassword.equals(hashedPassword);
  }
}
