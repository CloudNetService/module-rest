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

import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodContext;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodContextDecorator;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodDescriptor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.NonNull;
import org.hibernate.validator.HibernateValidator;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;

public final class ValidationHandlerMethodContextDecorator implements HttpHandlerMethodContextDecorator {

  private final Supplier<Validator> validatorFactory;
  private final ViolationResponseFactory violationResponseFactory;

  private ValidationHandlerMethodContextDecorator(
    @NonNull Supplier<Validator> validatorFactory,
    @NonNull ViolationResponseFactory violationResponseFactory
  ) {
    this.validatorFactory = validatorFactory;
    this.violationResponseFactory = violationResponseFactory;
  }

  public static @NonNull ValidationHandlerMethodContextDecorator withDefaultValidator() {
    var factory = Validation.byProvider(HibernateValidator.class)
      .configure()
      .defaultLocale(Locale.ENGLISH)
      .buildValidatorFactory();
    return ofValidatorFactory(factory);
  }

  public static @NonNull ValidationHandlerMethodContextDecorator ofValidator(@NonNull Validator validator) {
    return ofValidatorFactory(() -> validator);
  }

  public static @NonNull ValidationHandlerMethodContextDecorator ofValidatorFactory(@NonNull ValidatorFactory factory) {
    return ofValidatorFactory(factory::getValidator);
  }

  public static @NonNull ValidationHandlerMethodContextDecorator ofValidatorFactory(
    @NonNull Supplier<Validator> factory
  ) {
    return new ValidationHandlerMethodContextDecorator(factory, DefaultViolationResponseFactory.INSTANCE);
  }

  private static @Nullable EnableValidation extractValidationAnnotation(@NonNull Method method) {
    // first check the method directly to not lose potentially overridden configuration
    var directOnMethod = method.getAnnotation(EnableValidation.class);
    if (directOnMethod != null) {
      return directOnMethod;
    }

    // check for the presence on the type that declares the method
    var declaringType = method.getDeclaringClass();
    return declaringType.getAnnotation(EnableValidation.class);
  }

  @CheckReturnValue
  public @NonNull ValidationHandlerMethodContextDecorator withViolationResponseFactory(
    @NonNull ViolationResponseFactory responseFactory
  ) {
    return new ValidationHandlerMethodContextDecorator(this.validatorFactory, responseFactory);
  }

  @Override
  public void decorateContext(
    @NonNull HttpHandlerMethodDescriptor methodDescriptor,
    @NonNull HttpHandlerMethodContext.Builder contextBuilder
  ) {
    var method = methodDescriptor.wrappedMethod();
    var enableValidationAnnotation = extractValidationAnnotation(method);

    // check if either the method or class has validation enabled
    if (enableValidationAnnotation != null) {
      var validator = this.validatorFactory.get();
      contextBuilder.addParamInterceptor(new ValidationHttpMethodParamInterceptor(
        enableValidationAnnotation.validationGroups(),
        validator.forExecutables(),
        this.violationResponseFactory));
    }
  }
}
