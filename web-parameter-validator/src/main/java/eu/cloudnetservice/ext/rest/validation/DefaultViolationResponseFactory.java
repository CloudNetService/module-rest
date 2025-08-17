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

import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class DefaultViolationResponseFactory implements ViolationResponseFactory {

  public static final ViolationResponseFactory INSTANCE = new DefaultViolationResponseFactory();

  private static final URI VALIDATION_PROBLEM = URI.create("parameter-validation-failure");

  private DefaultViolationResponseFactory() {
  }

  @Override
  public @NonNull IntoResponse<?> convertConstraintViolations(@NonNull Set<ConstraintViolation<Object>> violations) {
    var prettyViolations = violations.stream().map(this::prettifyConstraintViolation).toList();
    return ProblemDetail.builder()
      .type(VALIDATION_PROBLEM)
      .status(HttpResponseCode.BAD_REQUEST)
      .title("Invalid request parameter(s) provided")
      .detail("Passed request parameter(s) did not pass constraint validation")
      .addAdditionalField("issues", prettyViolations)
      .build();
  }

  private @NonNull Map<String, String> prettifyConstraintViolation(@NonNull ConstraintViolation<?> violation) {
    var prettyPath = this.prettifyPath(violation.getLeafBean(), violation.getPropertyPath());
    return Map.of(
      "path", prettyPath,
      "issue", violation.getMessage()
    );
  }

  private @NonNull String prettifyPath(@Nullable Object bean, @NonNull Path propertyPath) {
    // construct a joiner and use an empty string as the default value for further checks
    var joiner = new StringJoiner(".", bean == null ? "" : bean.getClass().getSimpleName() + '.', "");
    joiner.setEmptyValue("");

    for (var node : propertyPath) {
      // skip some types of node
      var kind = node.getKind();
      if (kind == ElementKind.METHOD
        || kind == ElementKind.CONSTRUCTOR
        || kind == ElementKind.CROSS_PARAMETER
        || kind == ElementKind.RETURN_VALUE) {
        continue;
      }

      // append the node if it helps for the path understanding
      var nodeAsString = node.toString();
      if (!nodeAsString.isBlank()) {
        joiner.add(nodeAsString);
      }
    }

    // return the default path in case no elements were added to the string joiner
    // note that this only works due to the fact that the empty value of the joiner is an empty string, as the joiner
    // returns the length of the empty value in case nothing was added
    if (joiner.length() == 0) {
      return propertyPath.toString();
    } else {
      return joiner.toString();
    }
  }
}
