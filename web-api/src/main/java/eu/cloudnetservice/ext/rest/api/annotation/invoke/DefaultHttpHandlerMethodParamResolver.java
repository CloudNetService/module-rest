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

package eu.cloudnetservice.ext.rest.api.annotation.invoke;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.ParameterInvocationHint;
import eu.cloudnetservice.ext.rest.api.problem.StandardProblemDetail;
import java.lang.reflect.Method;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

record DefaultHttpHandlerMethodParamResolver(
  @Nullable Integer contextArgPos,
  @Nullable Integer requestArgPos,
  @Nullable Integer responseArgPos,
  @NonNull Method handlerMethod,
  @NonNull Class<?>[] handlerParameterTypes
) implements HttpHandlerMethodParamResolver {

  public static @NonNull HttpHandlerMethodParamResolver fromMethod(@NonNull Method method) {
    // positions of fixed arguments
    Integer contextArgPos = null;
    Integer requestArgPos = null;
    Integer responseArgPos = null;

    // locate the indexes of the context, request and response argument types, if given
    var methodParamTypes = method.getParameterTypes();
    for (var paramIndex = 0; paramIndex < methodParamTypes.length; paramIndex++) {
      var paramType = methodParamTypes[paramIndex];
      if (HttpContext.class.isAssignableFrom(paramType)) {
        // http context argument
        Preconditions.checkArgument(contextArgPos == null, "Found duplicate context argument");
        contextArgPos = paramIndex;
      } else if (HttpRequest.class.isAssignableFrom(paramType)) {
        // http request argument
        Preconditions.checkArgument(requestArgPos == null, "Found duplicate request argument");
        requestArgPos = paramIndex;
      } else if (HttpResponse.class.isAssignableFrom(paramType)) {
        // http response argument
        Preconditions.checkArgument(responseArgPos == null, "Found duplicate response argument");
        responseArgPos = paramIndex;
      }
    }

    return new DefaultHttpHandlerMethodParamResolver(
      contextArgPos,
      requestArgPos,
      responseArgPos,
      method,
      methodParamTypes);
  }

  @Override
  public void resolveMethodParameter(
    @NonNull HttpContext context,
    @NonNull HttpHandlerMethodDescriptor methodDescriptor,
    Object[] params
  ) {
    // register the method parameters that are passed in via invocation hint
    var invocationHints = context.invocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY);
    for (var invocationHint : invocationHints) {
      // validate that we got and invocation hint
      if (invocationHint instanceof ParameterInvocationHint hint) {
        // validate that the value type is matching the expected type at the index
        var value = hint.resolveValue(context);
        var expectedType = this.handlerParameterTypes[hint.index()];
        if (value != null && !expectedType.isAssignableFrom(value.getClass())) {
          throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
            .handlerMethod(this.handlerMethod)
            .debugDescription(String.format(
              "Expected value of type %s; got %s for param at index %d",
              expectedType.getSimpleName(), value.getClass().getSimpleName(), hint.index()))
            .build();
        }

        // don't accidentally try to inject null into a primitive type
        if (value == null && expectedType.isPrimitive()) {
          throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
            .handlerMethod(this.handlerMethod)
            .debugDescription(String.format(
              "Parameter at index %d is primitive (%s) but null was resolved as the parameter value",
              hint.index(), expectedType.getSimpleName()))
            .build();
        }

        // all fine, store the argument
        params[hint.index()] = value;
      } else {
        throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
          .handlerMethod(this.handlerMethod)
          .debugDescription(String.format("Hint %s is not a ParameterInvocationHint", invocationHint.getClass()))
          .build();
      }
    }

    // register the pre-resolved argument types
    this.registerDirectArguments(context, params);
  }

  private void registerDirectArguments(@NonNull HttpContext context, @NonNull Object[] params) {
    if (this.contextArgPos != null) {
      params[this.contextArgPos] = context;
    }

    if (this.requestArgPos != null) {
      params[this.requestArgPos] = context.request();
    }

    if (this.responseArgPos != null) {
      params[this.responseArgPos] = context.response();
    }
  }
}
