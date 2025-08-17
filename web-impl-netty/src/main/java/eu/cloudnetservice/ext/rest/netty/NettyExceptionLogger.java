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

import io.netty5.channel.ChannelException;
import io.netty5.handler.timeout.ReadTimeoutException;
import io.netty5.util.concurrent.FutureListener;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import javax.net.ssl.SSLException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for logging exceptions that occur while processing a request.
 *
 * @since 1.0
 */
final class NettyExceptionLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyExceptionLogger.class);

  /**
   * A future listener that logs the exception produced by the future to which it was attached if it is relevant enough.
   * If the exception is deemed to be irrelevant in the current context, it is silently ignored.
   */
  public static final FutureListener<Object> LOG_ON_FAILURE = future -> {
    if (future.isFailed() && !future.isCancelled()) {
      NettyExceptionLogger.handleConnectionException(future.cause());
    }
  };

  private NettyExceptionLogger() {
    throw new UnsupportedOperationException();
  }

  /**
   * Logs the given exception if it is relevant enough, silently ignoring it otherwise.
   *
   * @param cause the cause to log if it is relevant enough.
   * @throws NullPointerException if the given cause is null.
   */
  static void handleConnectionException(@NonNull Throwable cause) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Caught exception while processing rest request", cause);
      return;
    }

    if (cause instanceof ClosedChannelException || cause instanceof ReadTimeoutException) {
      // happens when trying to write to a channel that was improperly closed by the remote
      return;
    }

    var message = cause.getMessage();
    if (message != null) {
      var lowerMessage = message.toLowerCase(Locale.ROOT);
      if (cause instanceof SSLException && lowerMessage.contains("closed already")) {
        // happens when remote closes the connection while ssl-related work is in progress
        return;
      }

      if ((cause instanceof IOException || cause instanceof ChannelException) && canIgnoreException(lowerMessage)) {
        // some sort of socket error that can safely be ignored
        return;
      }
    }

    LOGGER.warn("Caught exception while processing rest request", cause);
  }

  /**
   * Checks if the given error associated with the given message can be safely ignored.
   *
   * @param message the error message to check.
   * @return true if the error associated with the given message can be safely ignored, false otherwise.
   * @throws NullPointerException if the given message is null.
   */
  private static boolean canIgnoreException(@NonNull String message) {
    var broken = message.contains("broken");
    if (broken && message.contains("pipe")) {
      return true;
    }

    if (!message.contains("connection")) {
      return false;
    }

    // connection related with an ignorable state
    return broken || message.contains("closed") || message.contains("reset") || message.contains("abort");
  }
}
