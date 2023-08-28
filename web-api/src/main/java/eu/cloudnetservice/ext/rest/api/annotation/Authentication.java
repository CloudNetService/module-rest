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

import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
public @interface Authentication {

  String[] providers();

  /**
   * The scopes that are enforced on the http handler annotated with this annotation. The caller of this handler must
   * have at least one of the given scopes in order to successfully call this handler.
   * <p>
   * Note: All supplied scopes have to follow the scope pattern described in
   * {@link RestUserManagement#SCOPE_NAMING_REGEX}.
   *
   * @return the scopes that are enforced on the http handler annotated with this annotation.
   */
  String[] scopes() default {};
}