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

package eu.cloudnetservice.ext.rest.validation;

import eu.cloudnetservice.ext.rest.api.HttpHandleException;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodDescriptor;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodParamInterceptor;
import jakarta.validation.executable.ExecutableValidator;
import lombok.NonNull;

record ValidationHttpMethodParamInterceptor(
  @NonNull Class<?>[] validationGroups,
  @NonNull ExecutableValidator validator,
  @NonNull ViolationResponseFactory violationResponseFactory
) implements HttpHandlerMethodParamInterceptor {

  @Override
  public void interceptMethodParameters(
    @NonNull HttpHandlerMethodDescriptor methodDescriptor,
    @NonNull Object[] params
  ) {
    var handerMethod = methodDescriptor.wrappedMethod();
    var handlerInstance = methodDescriptor.definingInstance();

    // 1. checks if there are actually any constraint violations before passing to the response factory
    // 2. validate that the response factory is willing to break the processing due to the violations
    var validationResult = this.validator.validateParameters(
      handlerInstance,
      handerMethod,
      params,
      this.validationGroups);
    if (!validationResult.isEmpty()) {
      var response = this.violationResponseFactory.convertConstraintViolations(validationResult);
      if (response != null) {
        throw new HttpHandleException(response);
      }
    }
  }
}
