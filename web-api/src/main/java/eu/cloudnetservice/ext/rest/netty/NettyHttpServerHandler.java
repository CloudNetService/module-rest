/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.cors.CorsRequestProcessor;
import eu.cloudnetservice.ext.rest.http.cors.DefaultCorsRequestProcessor;
import eu.cloudnetservice.ext.rest.http.response.Response;
import eu.cloudnetservice.ext.rest.http.tree.HttpHandlerConfigPair;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFutureListeners;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import io.netty5.handler.codec.http.DefaultHttpResponse;
import io.netty5.handler.codec.http.EmptyLastHttpContent;
import io.netty5.handler.codec.http.HttpChunkedInput;
import io.netty5.handler.codec.http.HttpHeaderNames;
import io.netty5.handler.codec.http.HttpHeaderValues;
import io.netty5.handler.codec.http.HttpRequest;
import io.netty5.handler.codec.http.HttpResponseStatus;
import io.netty5.handler.codec.http.HttpUtil;
import io.netty5.handler.stream.ChunkedStream;
import io.netty5.handler.timeout.ReadTimeoutException;
import io.netty5.util.AttributeKey;
import io.netty5.util.concurrent.Future;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The http server handler implementation responsible to handling http requests sent to the server and responding to
 * them.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class NettyHttpServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

  public static final AttributeKey<HostAndPort> PROXY_REMOTE_ADDRESS_KEY = AttributeKey.valueOf("PROXY_REMOTE_ADDRESS");

  private static final Logger LOGGER = LogManager.logger(NettyHttpServerHandler.class);

  private final CorsRequestProcessor corsRequestProcessor;

  private final NettyHttpServer nettyHttpServer;
  private final HostAndPort connectedAddress;

  private NettyHttpChannel channel;

  /**
   * Constructs a new http server handler instance.
   *
   * @param nettyHttpServer  the http server associated with this handler.
   * @param connectedAddress the listener host and port associated with this handler.
   * @throws NullPointerException if the given server or host and port are null.
   */
  public NettyHttpServerHandler(@NonNull NettyHttpServer nettyHttpServer, @NonNull HostAndPort connectedAddress) {
    this.corsRequestProcessor = new DefaultCorsRequestProcessor();
    this.nettyHttpServer = nettyHttpServer;
    this.connectedAddress = connectedAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      ctx.channel().close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelExceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException) && !(cause instanceof ReadTimeoutException)) {
      LOGGER.severe("Exception caught during processing of http request", cause);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelReadComplete(@NonNull ChannelHandlerContext ctx) {
    ctx.flush();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void messageReceived(@NonNull ChannelHandlerContext ctx, @NonNull HttpRequest msg) {
    // validate that the request was actually decoded before processing
    if (msg.decoderResult().isFailure()) {
      ctx.channel().close();
      return;
    }

    this.handleMessage(ctx.channel(), msg);
  }

  /**
   * Handles an incoming http request, posting it to the correct handler while parsing everything from it beforehand.
   *
   * @param channel     the channel from which the request came.
   * @param httpRequest the decoded request to handle.
   * @throws NullPointerException if the given channel or request is null.
   */
  private void handleMessage(@NonNull Channel channel, @NonNull HttpRequest httpRequest) {
    // if an opaque uri is sent to the server we reject the request immediately as it does
    // not contain the required information to properly process the request (especially due
    // to the lack of path information which is the base of our internal handling)
    var uri = URI.create(httpRequest.uri());
    if (uri.isOpaque()) {
      channel
        .writeAndFlush(new DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.BAD_REQUEST))
        .addListener(channel, ChannelFutureListeners.CLOSE);
      return;
    }

    // check if the HttpChannel for this channel wasn't constructed yet - do that if needed now
    if (this.channel == null) {
      // get the client address of the channel - either from some proxy info or from the supplied client address
      var clientAddress = channel.attr(PROXY_REMOTE_ADDRESS_KEY).getAndSet(null);
      if (clientAddress == null) {
        clientAddress = HostAndPort.fromSocketAddress(channel.remoteAddress());
      }

      // get the request scheme and construct the channel info
      var requestScheme = this.nettyHttpServer.sslEnabled() ? "https" : "http";
      this.channel = new NettyHttpChannel(channel, requestScheme, this.connectedAddress, clientAddress);
    }

    // build the handling context
    var context = new NettyHttpServerContext(this.nettyHttpServer, this.channel, uri, new HashMap<>(), httpRequest);

    // find the node that is responsible to handle the request
    var fullPath = uri.getPath();
    var matchingTreeNode = this.nettyHttpServer.handlerTree;
    if (!fullPath.equals("/")) {
      // strip the leading slash
      if (fullPath.startsWith("/")) {
        fullPath = fullPath.substring(1);
      }

      for (var pathPart : fullPath.split("/")) {
        // find the first node that accepts the path part as input; returns null in case no node does so
        matchingTreeNode = matchingTreeNode.findMatchingChildNode(
          node -> node.pathNode().validateAndRegisterPathPart(context, pathPart));
        if (matchingTreeNode == null || matchingTreeNode.pathNode().consumesRemainingPath()) {
          break;
        }
      }
    }

    if (matchingTreeNode == null) {
      channel
        .writeAndFlush(new DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND))
        .addListener(channel, ChannelFutureListeners.CLOSE);
      return;
    }

    var preflightRequestInfo = this.corsRequestProcessor.extractInfoFromPreflightRequest(context.request());
    if (preflightRequestInfo != null) {
      // preflight request info is present, respond accordingly to the request
      var targetHandler = matchingTreeNode.pathNode().findHandlerForMethod(preflightRequestInfo.requestMethod());
      var handlerConfig = targetHandler != null ? targetHandler.config() : null;
      this.corsRequestProcessor.processPreflightRequest(context, preflightRequestInfo, handlerConfig);
    } else {
      // validate that the target handler for the request is present
      var targetHandler = matchingTreeNode.pathNode().findHandlerForMethod(httpRequest.method().name());
      if (targetHandler == null) {
        channel
          .writeAndFlush(new DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND))
          .addListener(channel, ChannelFutureListeners.CLOSE);
        return;
      }

      // validate that the request conforms to the CORS policy before handling
      if (this.corsRequestProcessor.processNormalRequest(context, targetHandler.config())) {
        var handlerResponse = this.postRequestToHandler(context, targetHandler);
        if (handlerResponse != null) {
          handlerResponse.serializeIntoResponse(context.response());
        }
      }
    }

    // check if the response set in the context should actually be transferred to the client
    if (!context.cancelSendResponse) {
      var response = context.httpServerResponse;

      // append the keep-alive header if requested
      var netty = response.httpResponse;
      if (!context.closeAfter) {
        netty.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      }

      // transfer the data chunked to the client if a response stream was set, indicating a huge data chunk
      Future<Void> future;
      if (response.bodyStream() != null) {
        // set the chunk transfer header
        HttpUtil.setTransferEncodingChunked(netty, true);

        // write the initial response to the client, use a void future as no monitoring is required
        channel.write(new DefaultHttpResponse(netty.protocolVersion(), netty.status(), netty.headers()));
        // write the actual content of the transfer into the channel using a progressive future
        future = channel.writeAndFlush(new HttpChunkedInput(
          new ChunkedStream(response.bodyStream()),
          new EmptyLastHttpContent(channel.bufferAllocator())));
      } else {
        // do not mark the request data as chunked
        HttpUtil.setTransferEncodingChunked(netty, false);

        // Set the content length of the response and transfer the data to the client
        HttpUtil.setContentLength(netty, netty.payload().readableBytes());
        future = channel.writeAndFlush(netty);
      }

      // add the listener that fires the exception if an error occurs during writing of the response
      future.addListener(channel, ChannelFutureListeners.FIRE_EXCEPTION_ON_FAILURE);
      if (context.closeAfter) {
        future.addListener(channel, ChannelFutureListeners.CLOSE);
      }
    }
  }

  private @Nullable Response<?> postRequestToHandler(
    @NonNull HttpContext context,
    @NonNull HttpHandlerConfigPair handlerConfigPair
  ) {
    var config = handlerConfigPair.config();
    var httpHandler = handlerConfigPair.httpHandler();

    try {
      // post the context to the invocation handlers (if any registered)
      if (!config.invokePreProcessors(context, httpHandler, config)) {
        return null;
      }

      // post the request to the actual handler
      var response = httpHandler.handle(context).intoResponse();

      // post process the response
      var returnAllowed = config.invokePostProcessors(context, httpHandler, config, response);
      return returnAllowed ? response : null;
    } catch (Throwable throwable) {
      // post the exception to the handlers
      try {
        config.invokeExceptionallyPostProcessors(context, httpHandler, config, throwable);
      } catch (Exception exception) {
        // unable to handle the exception
        LOGGER.fine("Exception in post-processing exception handler", exception);
        context.response().status(HttpResponseCode.INTERNAL_SERVER_ERROR);
      }
    }

    return null;
  }
}
