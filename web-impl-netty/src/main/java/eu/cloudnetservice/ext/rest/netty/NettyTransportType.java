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

import com.google.common.base.Suppliers;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.IoHandlerFactory;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.ServerChannel;
import io.netty5.channel.ServerChannelFactory;
import io.netty5.channel.epoll.Epoll;
import io.netty5.channel.epoll.EpollIoHandler;
import io.netty5.channel.epoll.EpollServerSocketChannel;
import io.netty5.channel.kqueue.KQueue;
import io.netty5.channel.kqueue.KQueueIoHandler;
import io.netty5.channel.kqueue.KQueueServerSocketChannel;
import io.netty5.channel.nio.NioIoHandler;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * A collection of transport types that are supported and can be used.
 *
 * @since 1.0
 */
enum NettyTransportType {

  EPOLL(
    "epoll",
    Epoll.isAvailable(),
    true,
    EpollIoHandler::newFactory,
    EpollServerSocketChannel::new
  ),
  KQUEUE(
    "kqueue",
    KQueue.isAvailable(),
    true,
    KQueueIoHandler::newFactory,
    KQueueServerSocketChannel::new
  ),
  NIO(
    "nio",
    true,
    false,
    NioIoHandler::newFactory,
    NioServerSocketChannel::new
  );

  private final String name;
  private final boolean available;
  private final boolean nativeTransport;
  private final Supplier<IoHandlerFactory> ioHandlerFactory;
  private final ServerChannelFactory<? extends ServerChannel> serverChannelFactory;

  /**
   * Constructs a new netty transport instance.
   *
   * @param name                 the display name of the transport.
   * @param available            if the transport is available.
   * @param nativeTransport      if the transport is native.
   * @param ioHandlerFactory     the factory for io handlers.
   * @param serverChannelFactory the factory for server channels.
   * @throws NullPointerException if one of the given parameters is null.
   */
  NettyTransportType(
    @NonNull String name,
    boolean available,
    boolean nativeTransport,
    @NonNull Supplier<IoHandlerFactory> ioHandlerFactory,
    @NonNull ServerChannelFactory<? extends ServerChannel> serverChannelFactory
  ) {
    this.name = name;
    this.available = available;
    this.nativeTransport = nativeTransport;
    this.ioHandlerFactory = Suppliers.memoize(ioHandlerFactory::get);
    this.serverChannelFactory = serverChannelFactory;
  }

  /**
   * Selects and returns the first available transport. If this method should not return native transports, it currently
   * only returns nio.
   *
   * @param noNative if no native transport should get included into the selection.
   * @return the first available transport.
   * @throws IllegalStateException if no transport is available, should normally never happen.
   */
  public static @NonNull NettyTransportType availableTransport(boolean noNative) {
    for (var transport : values()) {
      // ignore native transports if no-native is selected
      if (noNative && transport.nativeTransport()) {
        continue;
      }

      // use the first available transport
      if (transport.available) {
        return transport;
      }
    }

    // unable to find a transport?
    throw new IllegalStateException("Unable to select an available netty transport!");
  }

  /**
   * Creates a new event loop group of the current selected transport with the supplied amount of threads.
   *
   * @param threads the amount of threads.
   * @return a new event loop group for this transport.
   * @throws IllegalArgumentException if the given number of threads is negative.
   */
  public @NonNull EventLoopGroup createEventLoopGroup(int threads) {
    return new MultithreadEventLoopGroup(threads, this.ioHandlerFactory.get());
  }

  /**
   * Gets the display name of the transport.
   *
   * @return the display name.
   */
  public @NonNull String displayName() {
    return this.name;
  }

  /**
   * Gets if this transport type is native.
   *
   * @return if this transport type is native.
   */
  public boolean nativeTransport() {
    return this.nativeTransport;
  }

  /**
   * Gets the factory for server channels of this transport.
   *
   * @return the factory for server channels of this transport.
   */
  public @NonNull ServerChannelFactory<? extends ServerChannel> serverChannelFactory() {
    return this.serverChannelFactory;
  }
}
