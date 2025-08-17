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

package eu.cloudnetservice.ext.rest.netty;

import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import io.netty.contrib.handler.codec.haproxy.HAProxyMessage;
import io.netty5.channel.ChannelHandler;
import io.netty5.channel.ChannelHandlerContext;
import lombok.NonNull;

final class NettyHAProxyMessageHandler implements ChannelHandler {

  public static final ChannelHandler INSTANCE = new NettyHAProxyMessageHandler();

  private NettyHAProxyMessageHandler() {
  }

  @Override
  public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg) throws Exception {
    if (msg instanceof HAProxyMessage proxyMessage) {
      try (proxyMessage) {
        var sourcePort = proxyMessage.sourcePort();
        var sourceAddress = proxyMessage.sourceAddress();

        // check if both source port and source address are given - set the proxy address in that case
        if (sourcePort != 0 && sourceAddress != null) {
          var sourceHostAndPort = new HostAndPort(sourceAddress, sourcePort);
          ctx.channel().attr(NettyHttpServerHandler.PROXY_REMOTE_ADDRESS_KEY).set(sourceHostAndPort);
        }
      }

      // there should only be one proxy message per invocation, remove our handler
      ctx.channel().pipeline().remove(this);
      ctx.read();
    } else {
      // just pass the message to the next handlers
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
