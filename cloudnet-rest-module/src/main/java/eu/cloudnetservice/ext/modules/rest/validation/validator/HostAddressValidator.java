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

package eu.cloudnetservice.ext.modules.rest.validation.validator;

import eu.cloudnetservice.ext.modules.rest.validation.HostAddress;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NonNull;

public class HostAddressValidator implements ConstraintValidator<HostAddress, String> {

  @Override
  public boolean isValid(@NonNull String value, @NonNull ConstraintValidatorContext context) {
    var hostAddress = NetworkUtil.parseHostAndPort(value, false);
    return hostAddress != null;
  }
}
