/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.rest.api.annotation;

import eu.cloudnetservice.ext.rest.api.HttpMethod;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Represents a method which can handle http requests sent to one of the provided paths using and request methods. The
 * first parameter of an annotated method must (and will) be the request HttpContext.
 *
 * @since 1.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHandler {

  /**
   * Get the paths to which the request can be sent in order to call the associated handling method.
   *
   * @return the url paths the associated method is handling.
   */
  @NonNull String path();

  /**
   * Get the method which can be used to call the associated handling method, defaults to GET.
   *
   * @return the http request method the associated method is handling.
   */
  @NonNull HttpMethod method() default HttpMethod.GET;
}
