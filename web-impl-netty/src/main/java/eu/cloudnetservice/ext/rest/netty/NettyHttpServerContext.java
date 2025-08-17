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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.cloudnetservice.ext.rest.api.HttpChannel;
import eu.cloudnetservice.ext.rest.api.HttpComponent;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpServer;
import eu.cloudnetservice.ext.rest.api.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketChannel;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.channel.Channel;
import io.netty5.handler.codec.http.DefaultFullHttpResponse;
import io.netty5.handler.codec.http.HttpResponseStatus;
import io.netty5.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty5.util.Send;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty implementation of a http handling context.
 *
 * @since 1.0
 */
final class NettyHttpServerContext implements HttpContext {

  final NettyHttpServerResponse httpServerResponse;
  final Multimap<String, Object> invocationHints = ArrayListMultimap.create();

  private final Channel nettyChannel;
  private final io.netty5.handler.codec.http.HttpRequest httpRequest;

  private final NettyHttpServer nettyHttpServer;
  private final BasicHttpConnectionInfo connectionInfo;
  private final NettyHttpServerRequest httpServerRequest;

  volatile boolean closeAfter = false;
  volatile boolean cancelSendResponse = false;

  private NettyHttpChannel channel;
  private volatile NettyWebSocketServerChannel webSocketServerChannel;

  /**
   * Constructs a new netty http server context instance.
   *
   * @param nettyHttpServer the http server which received the request handled by this context.
   * @param channel         the channel to which the request was sent.
   * @param uri             the uri of the request.
   * @param pathParameters  the path parameters pre-parsed, by default an empty map.
   * @param httpRequest     the http request which was received originally.
   * @param buffer          the buffer wrapped in a send which contains the request body.
   * @throws NullPointerException if one of the constructor parameters is null.
   */
  public NettyHttpServerContext(
    @NonNull NettyHttpServer nettyHttpServer,
    @NonNull NettyHttpChannel channel,
    @NonNull URI uri,
    @NonNull Map<String, String> pathParameters,
    @NonNull io.netty5.handler.codec.http.HttpRequest httpRequest,
    @Nullable Send<Buffer> buffer
  ) {
    this.nettyHttpServer = nettyHttpServer;
    this.channel = channel;
    this.httpRequest = httpRequest;
    this.nettyChannel = channel.channel();

    this.httpServerRequest = new NettyHttpServerRequest(this, httpRequest, pathParameters, uri, buffer);
    this.httpServerResponse = new NettyHttpServerResponse(this, httpRequest);

    // extract the requesting connection info
    var baseConnectInfo = new BasicHttpConnectionInfo(
      channel.scheme(),
      channel.serverAddress(),
      channel.clientAddress());
    this.connectionInfo = nettyHttpServer.componentConfig()
      .connectionInfoResolver()
      .extractConnectionInfo(this, baseConnectInfo);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<WebSocketChannel> upgrade() {
    if (this.webSocketServerChannel == null) {
      // not upgraded yet, build a new handshaker based on the given information
      var handshaker = new WebSocketServerHandshakerFactory(
        this.httpRequest.uri(),
        null,
        true,
        Short.MAX_VALUE,
        false
      ).newHandshaker(this.httpRequest);

      // no handshaker (as per the netty docs) means that the websocket version of the request is unsupported.
      // in both cases we don't want to respond - in case there is no handshaker we're sending out a response here,
      // in case we upgraded there is no need to send a response
      this.cancelSendResponse = true;
      if (handshaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(this.nettyChannel);
        return CompletableFuture.failedFuture(new IllegalStateException("Unsupported web socket version"));
      } else {
        // remove the http handler from the pipeline, gets replaced with the websocket one
        this.nettyChannel.pipeline().remove("http-server-handler");
        this.nettyChannel.pipeline().remove("read-timeout-handler");

        // try to greet the client
        CompletableFuture<WebSocketChannel> task = new CompletableFuture<>();
        handshaker.handshake(this.nettyChannel, this.httpRequest).addListener(future -> {
          if (future.isSuccess()) {
            // change the protocol of the http channel for wss
            this.channel = new NettyHttpChannel(
              this.channel.channel(),
              this.nettyHttpServer.sslEnabled() ? "wss" : "ws",
              this.channel.serverAddress(),
              this.channel.clientAddress());

            // successfully greeted the client, setup everything we need
            this.webSocketServerChannel = new NettyWebSocketServerChannel(this.channel, this.nettyChannel);
            this.nettyChannel.pipeline().addLast(
              "websocket-server-channel-handler",
              new NettyWebSocketServerChannelHandler(this.webSocketServerChannel));

            // done :)
            task.complete(this.webSocketServerChannel);
          } else {
            // something went wrong...
            this.nettyChannel.writeAndFlush(new DefaultFullHttpResponse(
              this.httpRequest.protocolVersion(),
              HttpResponseStatus.OK,
              DefaultBufferAllocators.offHeapAllocator().copyOf("Unable to upgrade connection".getBytes())
            ));
            task.completeExceptionally(future.cause());
          }
        });
        return task;
      }
    } else {
      return CompletableFuture.completedFuture(this.webSocketServerChannel);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable WebSocketChannel webSocketChannel() {
    return this.webSocketServerChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpChannel channel() {
    return this.channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest request() {
    return this.httpServerRequest;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse response() {
    return this.httpServerResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpComponent<HttpServer> component() {
    return this.nettyHttpServer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull BasicHttpConnectionInfo connectionInfo() {
    return this.connectionInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext closeAfter(boolean value) {
    this.closeAfter = value;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean closeAfter() {
    return this.closeAfter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Object> invocationHints(@NonNull String key) {
    return this.invocationHints.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext addInvocationHint(@NonNull String key, @NonNull Object value) {
    this.invocationHints.put(key, value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> HttpContext addInvocationHints(@NonNull String key, @NonNull Collection<T> value) {
    this.invocationHints.putAll(key, value);
    return this;
  }
}
