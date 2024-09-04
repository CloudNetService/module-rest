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

import eu.cloudnetservice.ext.rest.api.config.HttpProxyMode;
import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;
import io.netty5.handler.codec.http.HttpContentCompressor;
import io.netty5.handler.codec.http.HttpContentDecompressor;
import io.netty5.handler.codec.http.HttpRequestDecoder;
import io.netty5.handler.codec.http.HttpResponseEncoder;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.stream.ChunkedWriteHandler;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default channel initializer used to initialize http server connections.
 *
 * @since 1.0
 */
final class NettyHttpServerInitializer extends ChannelInitializer<Channel> {

  private final SslContext serverSslContext;
  private final HostAndPort listenerAddress;
  private final NettyHttpServer nettyHttpServer;

  private final ExecutorService executorService;

  private final int maxContentLength;

  /**
   * Constructs a new netty http server initializer instance.
   *
   * @param serverSslContext the ssl context to use for the http server, null if ssl is disabled.
   * @param nettyHttpServer  the http server the initializer belongs to.
   * @param listenerAddress  the host and port of the listener which was bound.
   * @param executorService  the executor service to use when handling requests.
   * @param maxContentLength the maximum size (in bytes) of the content the http server is allowed to handle.
   * @throws NullPointerException if either the http server or host and port is null.
   */
  public NettyHttpServerInitializer(
    @Nullable SslContext serverSslContext,
    @NonNull HostAndPort listenerAddress,
    @NonNull NettyHttpServer nettyHttpServer,
    @NonNull ExecutorService executorService,
    int maxContentLength
  ) {
    this.serverSslContext = serverSslContext;
    this.listenerAddress = listenerAddress;
    this.nettyHttpServer = nettyHttpServer;
    this.executorService = executorService;
    this.maxContentLength = maxContentLength;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initChannel(@NonNull Channel ch) {
    var componentConfig = this.nettyHttpServer.componentConfig();

    // add the HA proxy handler, if needed
    var haProxyMode = componentConfig.haProxyMode();
    if (haProxyMode != HttpProxyMode.DISABLED) {
      ch.pipeline().addLast(NettyHAProxySupportHandler.HANDLER_NAME, new NettyHAProxySupportHandler(haProxyMode));
    }

    // add the ssl handler if needed
    if (this.serverSslContext != null) {
      ch.pipeline().addLast("ssl-handler", this.serverSslContext.newHandler(ch.bufferAllocator()));
    }

    ch.pipeline()
      .addLast("read-timeout-handler", new NettyIdleStateHandler(30))
      .addLast("http-request-decoder", new HttpRequestDecoder())
      .addLast("http-request-decompressor", new HttpContentDecompressor())
      .addLast("http-response-encoder", new HttpResponseEncoder())
      .addLast("http-response-compressor", new HttpContentCompressor())
      .addLast("http-response-chunk-writer", new ChunkedWriteHandler())
      .addLast("http-object-aggregator", new NettyOversizedClosingHttpAggregator<>(this.maxContentLength))
      .addLast("http-server-handler", new NettyHttpServerHandler(
        this.nettyHttpServer,
        this.listenerAddress,
        this.executorService));
  }
}
