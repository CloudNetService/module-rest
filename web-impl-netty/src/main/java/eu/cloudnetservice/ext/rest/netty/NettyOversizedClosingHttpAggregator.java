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

import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.http.HttpContent;
import io.netty5.handler.codec.http.HttpObjectAggregator;
import io.netty5.handler.codec.http.HttpRequest;
import io.netty5.handler.codec.http.HttpResponseStatus;
import lombok.NonNull;

final class NettyOversizedClosingHttpAggregator<C extends HttpContent<C>> extends HttpObjectAggregator<C> {

  public NettyOversizedClosingHttpAggregator(int maxContentLength) {
    super(maxContentLength);
  }

  @Override
  protected void handleOversizedMessage(@NonNull ChannelHandlerContext ctx, @NonNull Object tooLarge) throws Exception {
    if (tooLarge instanceof HttpRequest) {
      // always the close the connection when the client sent a too large request body as this leaves
      // the decoder and this aggregator in a mismatched state which would cause the following request
      // with a too large body to hand forever
      NettyHttpServerUtil.sendResponseAndClose(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
    } else {
      // not a request, use the default handling
      super.handleOversizedMessage(ctx, tooLarge);
    }
  }
}
