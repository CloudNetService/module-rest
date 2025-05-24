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

import io.netty5.channel.ChannelException;
import io.netty5.handler.timeout.ReadTimeoutException;
import io.netty5.util.concurrent.FutureListener;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NettyExceptionLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyExceptionLogger.class);
  private static final Pattern IGNORABLE_SOCKET_ERROR_MESSAGE = Pattern.compile(
    "connection.*(?:reset|closed|abort|broken)|broken.*pipe", Pattern.CASE_INSENSITIVE);

  public static final FutureListener<Object> LOG_ON_FAILURE = (future -> {
    if (future.isFailed()) {
      NettyExceptionLogger.handleNettyException(future.cause());
    }
  });

  private NettyExceptionLogger() {
  }

  static void handleNettyException(@NonNull Throwable cause) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Caught exception while processing rest request", cause);
      return;
    }

    var shouldLog = switch (cause) {
      case ReadTimeoutException readTimeoutException -> false;
      case ClosedChannelException closedChannelException -> false;
      case SSLException sslException -> cause.getMessage() == null || !cause.getMessage().contains("closed already");
      case ChannelException channelException ->
        cause.getMessage() == null || !IGNORABLE_SOCKET_ERROR_MESSAGE.matcher(cause.getMessage()).find();
      case IOException ioException ->
        cause.getMessage() == null || !IGNORABLE_SOCKET_ERROR_MESSAGE.matcher(cause.getMessage()).find();
      default -> true;
    };

    if (shouldLog) {
      LOGGER.warn("Exception while processing rest request", cause);
    }
  }
}
