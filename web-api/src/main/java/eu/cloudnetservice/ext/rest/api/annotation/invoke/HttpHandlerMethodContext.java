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

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandleException;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.problem.StandardProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpHandlerMethodContext implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHandlerMethodContext.class);

  private final int targetMethodParamCount;
  private final String methodDebugRepresentation;
  private final HttpHandlerMethodDescriptor targetMethod;

  private final HttpHandlerMethodInvoker methodInvoker;
  private final HttpHandlerMethodParamResolver paramResolver;
  private final HttpHandlerMethodParamInterceptor paramInterceptor;

  private HttpHandlerMethodContext(
    @NonNull HttpHandlerMethodDescriptor targetMethod,
    @NonNull HttpHandlerMethodInvoker methodInvoker,
    @NonNull HttpHandlerMethodParamResolver paramResolver,
    @NonNull HttpHandlerMethodParamInterceptor paramInterceptor
  ) {
    this.targetMethod = targetMethod;
    this.methodInvoker = methodInvoker;
    this.paramResolver = paramResolver;
    this.paramInterceptor = paramInterceptor;

    this.targetMethodParamCount = targetMethod.wrappedMethod().getParameterCount();
    this.methodDebugRepresentation = DefaultHttpHandlerMethodInvoker.prettyPrintMethod(targetMethod.wrappedMethod());
  }

  @Override
  public @NonNull IntoResponse<?> handle(@NonNull HttpContext context) throws Exception {
    var methodParameters = new Object[this.targetMethodParamCount];

    // resolve the method parameters and call all argument interceptors
    this.paramResolver.resolveMethodParameter(context, this.targetMethod, methodParameters);
    this.paramInterceptor.interceptMethodParameters(this.targetMethod, methodParameters);

    var methodCallResult = this.callHandlerMethod(methodParameters);

    // method is not allowed to return null
    if (methodCallResult == null) {
      throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
        .handlerMethod(this.targetMethod.wrappedMethod())
        .debugDescription("Http handler method returned 'null' which is not allowed")
        .build();
    }

    // check if the response type is the one we're expecting
    if (!(methodCallResult instanceof IntoResponse<?>)) {
      throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
        .handlerMethod(this.targetMethod.wrappedMethod())
        .debugDescription("Http handler method returned '"
          + methodCallResult.getClass().getSimpleName()
          + "', should be a subtype of IntoResponse")
        .build();
    }

    // cast and return the method call result
    return (IntoResponse<?>) methodCallResult;
  }

  private @Nullable Object callHandlerMethod(@NonNull Object[] params) throws Exception {
    try {
      return this.methodInvoker.invokeHandlerMethod(this.targetMethod, params);
    } catch (ClassCastException | WrongMethodTypeException exception) {
      // this is not required to be caused by the handler invocation, this could possibly be propagated through the
      // actual method that was called (e.g. the method did a wrong cast). There might be some sneaky way to actually
      // filter out if an invalid argument type was passed, but for now we just assume it and move on
      // TODO(derklaro): maybe find a way to filter out actual illegal parameters
      throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
        .handlerMethod(this.targetMethod.wrappedMethod())
        .debugIssueCause(exception)
        .debugDescription("Possible argument type mismatch from handler param resolvers")
        .build();
    } catch (HttpHandleException exception) {
      // special case: we want to propagate this exception type unchanged to allow handlers to react to the information
      // that is wrapped in the exception (f. ex. a response status code or body)
      throw exception;
    } catch (Throwable throwable) {
      LOGGER.warn("Caught exception while invoking http handler method {}", this.methodDebugRepresentation, throwable);

      // something went wrong while invoking the method
      throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(StandardProblemDetail.INTERNAL_SERVER_ERROR)
        .handlerMethod(this.targetMethod.wrappedMethod())
        .debugIssueCause(throwable)
        .debugDescription("Caught exception while invoking http handler method")
        .build();
    }
  }

  @Override
  public @NonNull String toString() {
    return "HttpHandlerMethodContext for " + this.methodDebugRepresentation;
  }

  public static final class Builder {

    private static final HttpHandlerMethodParamInterceptor NO_OP_PARAM_INTERCEPTOR = (methodDescriptor, params) -> {
    };

    private final HttpHandlerMethodDescriptor targetMethod;

    private HttpHandlerMethodInvoker methodInvoker;
    private HttpHandlerMethodParamResolver paramResolver;
    private HttpHandlerMethodParamInterceptor paramInterceptor;

    public Builder(@NonNull HttpHandlerMethodDescriptor targetMethod) {
      this.targetMethod = targetMethod;
    }

    public @NonNull Builder methodInvoker(@NonNull HttpHandlerMethodInvoker methodInvoker) {
      this.methodInvoker = methodInvoker;
      return this;
    }

    public @NonNull Builder paramResolver(@NonNull HttpHandlerMethodParamResolver paramResolver) {
      this.paramResolver = paramResolver;
      return this;
    }

    public @NonNull Builder addParamResolver(@NonNull HttpHandlerMethodParamResolver paramResolver) {
      if (this.paramResolver == null) {
        this.paramResolver = paramResolver;
      } else {
        this.paramResolver = this.paramResolver.and(paramResolver);
      }

      return this;
    }

    public @NonNull Builder paramInterceptor(@NonNull HttpHandlerMethodParamInterceptor paramInterceptor) {
      this.paramInterceptor = paramInterceptor;
      return this;
    }

    public @NonNull Builder addParamInterceptor(@NonNull HttpHandlerMethodParamInterceptor paramInterceptor) {
      if (this.paramInterceptor == null) {
        this.paramInterceptor = paramInterceptor;
      } else {
        this.paramInterceptor = this.paramInterceptor.and(paramInterceptor);
      }

      return this;
    }

    public @NonNull HttpHandlerMethodContext build() {
      // use default values in case something wasn't specified
      var invoker = Objects.requireNonNullElseGet(
        this.methodInvoker,
        () -> DefaultHttpHandlerMethodInvoker.fromDescriptor(this.targetMethod));
      var paramResolver = Objects.requireNonNullElseGet(
        this.paramResolver,
        () -> DefaultHttpHandlerMethodParamResolver.fromMethod(this.targetMethod.wrappedMethod()));
      var paramInterceptor = Objects.requireNonNullElse(this.paramInterceptor, NO_OP_PARAM_INTERCEPTOR);

      // construct the final context
      return new HttpHandlerMethodContext(this.targetMethod, invoker, paramResolver, paramInterceptor);
    }
  }
}
