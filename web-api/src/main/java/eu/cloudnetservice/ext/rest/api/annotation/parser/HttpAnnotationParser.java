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

import eu.cloudnetservice.ext.rest.api.annotation.invoke.HttpHandlerMethodContextDecorator;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Collection;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A parser which can convert and register annotated http elements, supporting custom annotations as well.
 *
 * @since 1.0
 */
public interface HttpAnnotationParser {

  /**
   * Gets an unmodifiable view of all annotation processors which were registered to this parser.
   *
   * @return all annotation processors registered to this parser.
   */
  @UnmodifiableView
  @NonNull Collection<HttpAnnotationProcessor> annotationProcessors();

  /**
   * Registers an annotation processor to this parser.
   *
   * @param processor the processor to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given processor is null.
   */
  @NonNull HttpAnnotationParser registerAnnotationProcessor(@NonNull HttpAnnotationProcessor processor);

  /**
   * Unregisters an annotation processor from this parser if previously registered.
   *
   * @param processor the processor to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given processor is null.
   */
  @NonNull HttpAnnotationParser unregisterAnnotationProcessor(@NonNull HttpAnnotationProcessor processor);

  // TODO(derklaro): fix this documentation
  /*
   * Unregisters all annotation processors from this parser whose classes were loaded by the given class loader.
   *
   * @param filter the loader of the processor classes to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  @NonNull HttpAnnotationParser unregisterMatchingAnnotationProcessor(
    @NonNull Predicate<HttpAnnotationProcessor> filter);

  @UnmodifiableView
  @NonNull Collection<HttpHandlerMethodContextDecorator> handlerContextDecorators();

  @NonNull HttpAnnotationParser registerHandlerContextDecorator(@NonNull HttpHandlerMethodContextDecorator decorator);

  @NonNull HttpAnnotationParser unregisterHandlerContextDecorator(@NonNull HttpHandlerMethodContextDecorator decorator);

  @NonNull HttpAnnotationParser unregisterMatchingHandlerContextDecorator(
    @NonNull Predicate<HttpHandlerMethodContextDecorator> filter);

  /**
   * Parses all non-static http handlers methods annotated with {@code @HttpRequestHandler} in the given class instance.
   * This method will call all previously registered annotation processors and build context preprocessors from them,
   * then register the final parsed handler to the http component associated with this parser.
   *
   * @param handlerInstance the instance of the class in which the handler methods to register are located.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException        if the given handler instance is null.
   * @throws IllegalArgumentException    if annotating a static method, not taking a context as the first arg or if the
   *                                     annotation defines no paths or http methods to handle.
   * @throws InaccessibleObjectException if a http handler method is not accessible.
   */
  @NonNull HttpAnnotationParser parseAndRegister(@NonNull Object handlerInstance);
}
