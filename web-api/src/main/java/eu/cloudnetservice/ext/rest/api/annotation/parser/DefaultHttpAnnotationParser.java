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

package eu.cloudnetservice.ext.rest.api.annotation.parser;

import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodContext;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodContextDecorator;
import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodDescriptor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.AuthenticationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.ContentTypeProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.CrossOriginProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.FirstRequestQueryParamProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.RequestBodyProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.RequestHeaderProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.RequestPathParamProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.RequestPathProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.RequestQueryParamProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.processor.RequestTypedBodyProcessor;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.registry.HttpHandlerRegistry;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of a http annotation parser.
 *
 * @since 1.0
 */
public final class DefaultHttpAnnotationParser implements HttpAnnotationParser {

  public static final String DEFAULTS_TO_NULL_MASK = "__NULL__";
  public static final String PARAM_INVOCATION_HINT_KEY = "__PARAM_INVOCATION_HINT__";

  private final HttpHandlerRegistry httpHandlerRegistry;

  private final List<HttpAnnotationProcessor> processors = new ArrayList<>();
  private final List<HttpHandlerMethodContextDecorator> contextDecorators = new ArrayList<>();

  public DefaultHttpAnnotationParser(@NonNull HttpHandlerRegistry httpHandlerRegistry) {
    this.httpHandlerRegistry = httpHandlerRegistry;
  }

  /**
   * Returns the actual value if present, or the default value of an annotation. If the default value equals to
   * {@code __NULL__} then null is returned, else the given default value.
   *
   * @param defaultValue the defined default value to unmask.
   * @param actualValue  the actual value which is present from the request.
   * @return the actual value if present or the unmasked version of the default value.
   * @throws NullPointerException if the given default value is null.
   */
  public static @Nullable Object applyDefault(@NonNull String defaultValue, @Nullable String actualValue) {
    // return the actual value directly if present
    if (actualValue != null) {
      return actualValue;
    }

    // unmask the default value if needed and return it
    return defaultValue.equals(DEFAULTS_TO_NULL_MASK) ? null : defaultValue;
  }

  /**
   * Constructs a new DefaultHttpAnnotationParser instance and registers all processors for the default provided http
   * handling annotations. The annotation based http handlers are registered to the given handler registry.
   *
   * @param registry the registry the handlers are registered to.
   * @return the newly created HttpAnnotationParser instance.
   * @throws NullPointerException if the given registry is null.
   */
  public static @NonNull HttpAnnotationParser withDefaultProcessors(
    @NonNull HttpHandlerRegistry registry
  ) {
    return new DefaultHttpAnnotationParser(registry).registerDefaultProcessors();
  }

  /**
   * Registers all processors for the default provided http handling annotations.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull HttpAnnotationParser registerDefaultProcessors() {
    return this
      .registerAnnotationProcessor(new RequestBodyProcessor())
      .registerAnnotationProcessor(new RequestPathProcessor())
      .registerAnnotationProcessor(new ContentTypeProcessor())
      .registerAnnotationProcessor(new CrossOriginProcessor())
      .registerAnnotationProcessor(new RequestHeaderProcessor())
      .registerAnnotationProcessor(new RequestTypedBodyProcessor())
      .registerAnnotationProcessor(new RequestPathParamProcessor())
      .registerAnnotationProcessor(new RequestQueryParamProcessor())
      .registerAnnotationProcessor(new FirstRequestQueryParamProcessor())
      .registerAnnotationProcessor(new AuthenticationProcessor(RestUserManagementLoader::load));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnmodifiableView
  public @NonNull Collection<HttpAnnotationProcessor> annotationProcessors() {
    return Collections.unmodifiableCollection(this.processors);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser registerAnnotationProcessor(@NonNull HttpAnnotationProcessor processor) {
    this.processors.add(processor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser unregisterAnnotationProcessor(@NonNull HttpAnnotationProcessor processor) {
    this.processors.remove(processor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser unregisterMatchingAnnotationProcessor(
    @NonNull Predicate<HttpAnnotationProcessor> filter
  ) {
    this.processors.removeIf(filter);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnmodifiableView @NonNull Collection<HttpHandlerMethodContextDecorator> handlerContextDecorators() {
    return Collections.unmodifiableCollection(this.contextDecorators);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser registerHandlerContextDecorator(
    @NonNull HttpHandlerMethodContextDecorator decorator
  ) {
    this.contextDecorators.add(decorator);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser unregisterHandlerContextDecorator(
    @NonNull HttpHandlerMethodContextDecorator decorator
  ) {
    this.contextDecorators.remove(decorator);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser unregisterMatchingHandlerContextDecorator(
    @NonNull Predicate<HttpHandlerMethodContextDecorator> filter
  ) {
    this.contextDecorators.removeIf(filter);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser parseAndRegister(@NonNull Object handlerInstance) {
    var currentClass = handlerInstance.getClass();
    do {
      for (var method : currentClass.getDeclaredMethods()) {
        // check if the handler is requested to be a request handler
        var handlerAnnotation = method.getAnnotation(RequestHandler.class);
        if (handlerAnnotation != null) {
          var declaringClass = method.getDeclaringClass();

          // ensure that no abstract method is annotated
          if (Modifier.isAbstract(method.getModifiers())) {
            throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
              .handlerMethod(method)
              .annotationType(RequestHandler.class)
              .debugDescription("Http handler method is abstract and shouldn't be annotated")
              .build();
          }

          // disallow static methods completely
          if (Modifier.isStatic(method.getModifiers())) {
            throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
              .handlerMethod(method)
              .annotationType(RequestHandler.class)
              .debugDescription("Http handler method is static and shouldn't be annotated")
              .build();
          }

          // ensure that the handler method is accessible & public
          if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(declaringClass.getModifiers())) {
            throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
              .handlerMethod(method)
              .annotationType(RequestHandler.class)
              .debugDescription("Http handler method is exposed (method/class is not public)")
              .build();
          }

          // set the supported request method of the handler
          var configBuilder = HttpHandlerConfig.builder();
          configBuilder.httpMethod(handlerAnnotation.method());

          // add the processors to the corsConfig
          for (var processor : this.processors) {
            if (processor.shouldProcess(method, handlerInstance)) {
              processor.buildPreprocessor(configBuilder, method, handlerInstance);
            }
          }

          // build the final http handler and decorate the method handler
          var methodDescriptor = new HttpHandlerMethodDescriptor(method, handlerInstance);
          var contextBuilder = new HttpHandlerMethodContext.Builder(methodDescriptor);
          for (var contextDecorator : this.contextDecorators) {
            contextDecorator.decorateContext(methodDescriptor, contextBuilder);
          }

          // register the handler
          var handler = contextBuilder.build();
          this.httpHandlerRegistry.registerHandler(handlerAnnotation.path(), handler, configBuilder.build());
        }
      }
    } while ((currentClass = currentClass.getSuperclass()) != null);

    return this;
  }
}
