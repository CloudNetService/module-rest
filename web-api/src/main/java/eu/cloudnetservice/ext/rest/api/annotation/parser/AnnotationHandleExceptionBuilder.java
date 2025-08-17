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

import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.problem.ProblemHttpHandleException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A stackless exception which can be thrown when processing a http handler annotation.
 *
 * @since 1.0
 */
public final class AnnotationHandleExceptionBuilder {

  private final ProblemDetail problemDetail;

  private String debugDescription;
  private Throwable debugIssueCause;

  private Class<?> handlerClass;
  private String handlerMethodName;
  private String handlerMethodDesc;

  private String parameterName;
  private Class<?> parameterType;

  private Class<? extends Annotation> annotationType;

  private AnnotationHandleExceptionBuilder(@Nullable ProblemDetail problemDetail) {
    this.problemDetail = problemDetail;
  }

  public static @NonNull AnnotationHandleExceptionBuilder forIssueDuringRegistration() {
    return new AnnotationHandleExceptionBuilder(null);
  }

  public static @NonNull AnnotationHandleExceptionBuilder forIssueDuringRequest(@NonNull ProblemDetail problem) {
    return new AnnotationHandleExceptionBuilder(problem);
  }

  private static @NonNull String describeMethod(@NonNull Method method) {
    var parameterTypes = method.getParameterTypes();
    if (parameterTypes.length == 0) {
      // no param types
      return "():" + method.getReturnType().getSimpleName();
    }

    if (parameterTypes.length == 1) {
      // one param only
      return String.format("(%s):%s", parameterTypes[0].getSimpleName(), method.getReturnType().getSimpleName());
    }

    // multiple parameters, join them
    var paramJoiner = new StringJoiner(",");
    for (var type : parameterTypes) {
      paramJoiner.add(type.getSimpleName());
    }

    return String.format("(%s):%s", paramJoiner, method.getReturnType().getSimpleName());
  }

  public @NonNull AnnotationHandleExceptionBuilder debugDescription(@Nullable String debugDescription) {
    this.debugDescription = debugDescription;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder debugIssueCause(@Nullable Throwable debugIssueCause) {
    this.debugIssueCause = debugIssueCause;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder handlerMethod(@NonNull Method method) {
    this.handlerClass = method.getDeclaringClass();
    this.handlerMethodName = method.getName();
    this.handlerMethodDesc = describeMethod(method);
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder handlerClass(@Nullable Class<?> handlerClass) {
    this.handlerClass = handlerClass;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder handlerMethodName(@Nullable String handlerMethodName) {
    this.handlerMethodName = handlerMethodName;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder methodDesc(@Nullable String methodDesc) {
    this.handlerMethodDesc = methodDesc;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder parameter(@NonNull Parameter parameter) {
    this.parameterName = parameter.getName();
    this.parameterType = parameter.getType();
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder parameterName(@Nullable String parameterName) {
    this.parameterName = parameterName;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder parameterType(@Nullable Class<?> parameterType) {
    this.parameterType = parameterType;
    return this;
  }

  public @NonNull AnnotationHandleExceptionBuilder annotationType(@Nullable Class<? extends Annotation> annotation) {
    this.annotationType = annotation;
    return this;
  }

  public @NonNull RuntimeException build() {
    // describe the method
    var handlerMethodDescriptor = String.format(
      "HandlerMethod=[%s.%s(%s)]",
      Objects.requireNonNullElse(this.handlerClass, "???"),
      Objects.requireNonNullElse(this.handlerMethodName, "???"),
      Objects.requireNonNullElse(this.handlerMethodDesc, "???"));

    // describe the parameter
    var handlerParameterDescriptor = String.format(
      "Parameter=[%s %s]",
      Objects.requireNonNullElse(this.parameterType, "???"),
      Objects.requireNonNullElse(this.parameterName, "???"));

    // annotation type descriptor
    var annotationType = this.annotationType;
    var annotationDescriptor = annotationType == null ? "???" : '@' + annotationType.getSimpleName();

    // build the final debug messages
    var debugMessage = String.format(
      "There was a problem processing annotation %s: %s; %s; DetailDescription=%s",
      annotationDescriptor,
      handlerMethodDescriptor,
      handlerParameterDescriptor,
      Objects.requireNonNullElse(this.debugDescription, "???"));

    var problemDetail = this.problemDetail;
    if (problemDetail == null) {
      // thrown during registration
      return new IllegalArgumentException(debugMessage, this.debugIssueCause);
    } else {
      // thrown during request processing
      return new ProblemHttpHandleException(problemDetail, debugMessage, this.debugIssueCause);
    }
  }
}
