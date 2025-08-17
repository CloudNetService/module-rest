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

import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

record DefaultHttpHandlerMethodInvoker(@NonNull MethodHandle targetMethodHandle) implements HttpHandlerMethodInvoker {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

  public static @NonNull HttpHandlerMethodInvoker fromDescriptor(@NonNull HttpHandlerMethodDescriptor descriptor) {
    var method = descriptor.wrappedMethod();
    var declaringInstance = descriptor.definingInstance();

    try {
      // re-resolves the method to ensure that it is actually public and that the (possibly indirect) caller
      // of this method did not mess with the accessibility or visibility of the handler method before
      var staticMethod = Modifier.isStatic(method.getModifiers());
      var methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
      var resolvedMethodHandle = staticMethod
        ? LOOKUP.findStatic(method.getDeclaringClass(), method.getName(), methodType)
        : LOOKUP.findVirtual(method.getDeclaringClass(), method.getName(), methodType);

      // mark this method handler as a 'spreader'
      // this will make the method handle spread the input parameter array instead of interpreting it as a single param
      var spreadArgPos = staticMethod ? 0 : 1;
      var paramCount = resolvedMethodHandle.type().parameterCount() - (staticMethod ? 0 : 1);
      var targetMethodHandle = resolvedMethodHandle.asFixedArity().asSpreader(spreadArgPos, Object[].class, paramCount);

      if (staticMethod) {
        // first argument of the method handle is the 'this' parameter which we can ignore on static methods
        targetMethodHandle = MethodHandles.dropArguments(targetMethodHandle, 0, Object.class);
      } else {
        // bind the first argument ('this') to the instance of the class that declares the handler method
        targetMethodHandle = targetMethodHandle.bindTo(declaringInstance);
      }

      // generify the type of the method handle and return the final invoker instance
      var genericMethodType = MethodType.genericMethodType(staticMethod ? 1 : 0, true);
      targetMethodHandle = targetMethodHandle.asType(genericMethodType);
      return new DefaultHttpHandlerMethodInvoker(targetMethodHandle);
    } catch (IllegalAccessException exception) {
      throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
        .handlerMethod(method)
        .debugIssueCause(exception)
        .debugDescription("Handler method is not exported publicly")
        .build();
    } catch (NoSuchMethodException exception) {
      throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
        .handlerMethod(method)
        .debugIssueCause(exception)
        .debugDescription("Invalid method given (method somehow does not exist)")
        .build();
    }
  }

  static @NonNull String prettyPrintMethod(@NonNull Method method) {
    // pretty print the method arguments
    var joiner = new StringJoiner(", ");
    for (var parameterType : method.getParameterTypes()) {
      joiner.add(parameterType.getSimpleName());
    }

    // returns ClassName.MethodName(MethodParameterTypes)
    var declaringClass = method.getDeclaringClass().getSimpleName();
    return String.format("%s.%s(%s)", declaringClass, method.getName(), joiner);
  }

  @Override
  public @Nullable Object invokeHandlerMethod(
    @NonNull HttpHandlerMethodDescriptor methodDescriptor,
    @NonNull Object[] params
  ) throws Throwable {
    return this.targetMethodHandle.invokeExact(params);
  }
}
