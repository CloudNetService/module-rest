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

package eu.cloudnetservice.ext.modules.rest.dto;

import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import lombok.NonNull;

public final class SSLConfigurationDto implements Dto<SSLConfiguration> {

  private final boolean enabled;
  private final boolean clientAuth;
  @NotNull
  private final Path trustCertificatePath;
  @NotNull
  private final Path certificatePath;
  @NotNull
  private final Path privateKeyPath;
  private final String privateKeyPassword;

  public SSLConfigurationDto(
    boolean enabled,
    boolean clientAuth,
    Path trustCertificatePath,
    Path certificatePath,
    Path privateKeyPath,
    String privateKeyPassword
  ) {
    this.enabled = enabled;
    this.clientAuth = clientAuth;
    this.trustCertificatePath = trustCertificatePath;
    this.certificatePath = certificatePath;
    this.privateKeyPath = privateKeyPath;
    this.privateKeyPassword = privateKeyPassword;
  }

  @Override
  public @NonNull SSLConfiguration toEntity() {
    return new SSLConfiguration(
      this.enabled,
      this.clientAuth,
      this.trustCertificatePath,
      this.certificatePath,
      this.privateKeyPath,
      this.privateKeyPassword);
  }
}
