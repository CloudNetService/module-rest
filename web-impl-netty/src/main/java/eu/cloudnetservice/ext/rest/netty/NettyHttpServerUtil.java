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

package eu.cloudnetservice.ext.rest.netty;

import io.netty5.channel.ChannelOutboundInvoker;
import io.netty5.handler.codec.http.DefaultHttpResponse;
import io.netty5.handler.codec.http.HttpHeaderNames;
import io.netty5.handler.codec.http.HttpHeaderValues;
import io.netty5.handler.codec.http.HttpResponseStatus;
import io.netty5.handler.codec.http.HttpVersion;
import lombok.NonNull;

final class NettyHttpServerUtil {

  private NettyHttpServerUtil() {
    throw new UnsupportedOperationException();
  }

  public static void sendResponseAndClose(@NonNull ChannelOutboundInvoker channel, @NonNull HttpResponseStatus status) {
    var response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
    response.headers()
      .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
      .set(HttpHeaderNames.CONTENT_LENGTH, HttpHeaderValues.ZERO);
    channel.writeAndFlush(response).addListener(ignored -> channel.close());
  }
}
