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

import eu.cloudnetservice.ext.rest.api.HttpServer;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.SslConfiguration;
import eu.cloudnetservice.ext.rest.api.registry.HttpHandlerRegistry;
import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.handler.ssl.IdentityCipherSuiteFilter;
import io.netty5.handler.ssl.OpenSsl;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.SslProvider;
import io.netty5.util.concurrent.Future;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of the web server, using netty as its backing mechanism.
 *
 * @since 1.0
 */
final class NettyHttpServer implements HttpServer {

  private final SslContext sslContext;
  private final ComponentConfig componentConfig;

  private final Map<HostAndPort, Future<Void>> channelFutures = new ConcurrentHashMap<>();

  private final NettyTransportType transportType;
  private final EventLoopGroup bossEventLoopGroup;
  private final EventLoopGroup workerEventLoopGroup;

  private final HttpHandlerRegistry httpHandlerRegistry;
  private final HttpAnnotationParser annotationParser;

  /**
   * Constructs a new netty http server instance with the given ssl configuration.
   *
   * @param componentConfig the component configuration to use for the http server.
   * @throws NullPointerException     if the given configuration is null.
   * @throws IllegalArgumentException if ssl is enabled but cannot be initialized.
   */
  public NettyHttpServer(@NonNull ComponentConfig componentConfig) {
    this.componentConfig = componentConfig;
    this.httpHandlerRegistry = HttpHandlerRegistry.newHandlerRegistry(componentConfig);
    this.annotationParser = DefaultHttpAnnotationParser.withDefaultProcessors(this.httpHandlerRegistry);

    // init ssl
    this.sslContext = initSslContext(componentConfig.sslConfiguration());

    // select the available netty transport & create new a new event loop group with them
    this.transportType = NettyTransportType.availableTransport(componentConfig.disableNativeTransport());
    this.bossEventLoopGroup = this.transportType.createEventLoopGroup(1);
    this.workerEventLoopGroup = this.transportType.createEventLoopGroup(0);
  }

  private static @Nullable SslContext initSslContext(@Nullable SslConfiguration sslConfiguration) {
    if (sslConfiguration == null) {
      // ssl is disabled, nothing to do
      return null;
    } else {
      try (
        var keyStream = Files.newInputStream(sslConfiguration.keyPath(), StandardOpenOption.READ);
        var keyCertStream = Files.newInputStream(sslConfiguration.keyCertPath(), StandardOpenOption.READ)
      ) {
        return SslContextBuilder.forServer(keyCertStream, keyStream, sslConfiguration.keyPassword())
          .applicationProtocolConfig(null)
          .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
          .sslProvider(OpenSsl.isAvailable() ? SslProvider.OPENSSL : SslProvider.JDK)
          .build();
      } catch (IOException exception) {
        throw new IllegalArgumentException("Unable to construct server SSL context", exception);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean sslEnabled() {
    return this.sslContext != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ComponentConfig componentConfig() {
    return this.componentConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser annotationParser() {
    return this.annotationParser;
  }

  @Override
  public @NonNull HttpHandlerRegistry handlerRegistry() {
    return this.httpHandlerRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Void> addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Void> addListener(@NonNull HostAndPort hostAndPort) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    new ServerBootstrap()
      .group(this.bossEventLoopGroup, this.workerEventLoopGroup)
      .channelFactory(this.transportType.serverChannelFactory())
      .childHandler(new NettyHttpServerInitializer(
        this.sslContext,
        hostAndPort,
        this,
        this.componentConfig.executorService(),
        this.componentConfig.maxContentLength()))

      .childOption(ChannelOption.AUTO_READ, true)
      .childOption(ChannelOption.TCP_NODELAY, true)
      .childOption(ChannelOption.SO_REUSEADDR, true)

      .option(ChannelOption.SO_REUSEADDR, true)

      .bind(hostAndPort.host(), hostAndPort.port())
      .addListener(future -> {
        if (future.isSuccess()) {
          // ok, we bound successfully
          result.complete(null);
          this.channelFutures.put(hostAndPort, future.getNow().closeFuture());
        } else {
          // something went wrong
          result.completeExceptionally(future.cause());
        }
      });

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    for (var entry : this.channelFutures.values()) {
      entry.cancel();
    }

    this.bossEventLoopGroup.shutdownGracefully();
    this.workerEventLoopGroup.shutdownGracefully();
  }
}
