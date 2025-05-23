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

package eu.cloudnetservice.ext.rest.api.annotation.invoke;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;

// there is no constraint if the class impl should be constructed per-method or per-jvm
@FunctionalInterface
public interface HttpHandlerMethodParamResolver {

  void resolveMethodParameter(
    @NonNull HttpContext context,
    @NonNull HttpHandlerMethodDescriptor methodDescriptor,
    @NonNull Object[] params);

  @CheckReturnValue
  default @NonNull HttpHandlerMethodParamResolver and(@NonNull HttpHandlerMethodParamResolver other) {
    return (context, methodDescriptor, params) -> {
      this.resolveMethodParameter(context, methodDescriptor, params);
      other.resolveMethodParameter(context, methodDescriptor, params);
    };
  }
}
