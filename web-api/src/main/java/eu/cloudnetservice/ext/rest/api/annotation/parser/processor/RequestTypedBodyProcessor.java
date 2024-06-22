/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.rest.api.annotation.parser.processor;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHandleExceptionBuilder;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.codec.CodecLoader;
import eu.cloudnetservice.ext.rest.api.codec.DataformatCodec;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

public final class RequestTypedBodyProcessor implements HttpAnnotationProcessor {

  private static @NonNull Charset extractRequestCharsetOrUtf8(@NonNull HttpRequest request) {
    // get the content type header - if not present we just assume UTF8
    var contentType = request.headers().firstValue(HttpHeaders.CONTENT_TYPE);
    if (contentType == null) {
      return StandardCharsets.UTF_8;
    }

    try {
      var mediaType = MediaType.parse(contentType);
      return mediaType.charset().or(StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      return StandardCharsets.UTF_8;
    }
  }

  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handlerInstance
  ) {
    var hints = HttpAnnotationProcessorUtil.mapParameters(
      method,
      RequestTypedBody.class,
      (param, annotation) -> {
        var type = param.getParameterizedType();
        var deserializer = this.constructDeserializer(annotation.deserializationCodec(), method, param);

        return context -> {
          var request = context.request();
          var requestCharset = extractRequestCharsetOrUtf8(request);

          try {
            return deserializer.deserialize(requestCharset, type, request.bodyStream());
          } catch (Exception exception) {
            throw AnnotationHandleExceptionBuilder.forIssueDuringRequest(ProblemDetail.builder()
                .status(HttpResponseCode.BAD_REQUEST)
                .type("exception-during-decoding")
                .title("Exception during decoding")
                .detail("Unable to decode provided request body.")
                .build())
              .parameter(param)
              .handlerMethod(method)
              .debugIssueCause(exception)
              .annotationType(RequestTypedBody.class)
              .debugDescription(
                String.format(
                  "Calling the deserializer of type %s with the request body raised an exception.",
                  deserializer.getClass()))
              .build();
          }
        };
      });
    config.addHandlerInterceptor(new HttpHandlerInterceptor() {
      @Override
      public boolean preProcess(
        @NonNull HttpContext context,
        @NonNull HttpHandler handler,
        @NonNull HttpHandlerConfig config
      ) {
        context.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
        return true;
      }
    });
  }

  private @NonNull DataformatCodec constructDeserializer(
    @NonNull Class<? extends DataformatCodec> codecClass,
    @NonNull Method debugDeclaringMethod,
    @NonNull Parameter debugParameter
  ) {
    if (codecClass.isInterface()) {
      return CodecLoader.resolveCodec(codecClass);
    } else {
      try {
        // use the public no-args constructor in the codec class
        return codecClass.getConstructor().newInstance();
      } catch (NoSuchMethodException | IllegalAccessException exception) {
        throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
          .parameter(debugParameter)
          .handlerMethod(debugDeclaringMethod)
          .annotationType(RequestTypedBody.class)
          .debugDescription("Codec class " + codecClass + " does not define a public no-args constructor")
          .build();
      } catch (InstantiationException | InvocationTargetException exception) {
        throw AnnotationHandleExceptionBuilder.forIssueDuringRegistration()
          .parameter(debugParameter)
          .handlerMethod(debugDeclaringMethod)
          .annotationType(RequestTypedBody.class)
          .debugIssueCause(exception)
          .debugDescription("Codec class " + codecClass + " threw exception during instantiation")
          .build();
      }
    }
  }
}
