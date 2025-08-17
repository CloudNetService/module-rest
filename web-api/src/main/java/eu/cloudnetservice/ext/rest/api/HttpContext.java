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

package eu.cloudnetservice.ext.rest.api;

import eu.cloudnetservice.ext.rest.api.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketChannel;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The http context for requests and responses that are handled by this system. This context provides information needed
 * for the handling and processing of both requests and responses.
 * <p>
 * The context is unique for each incoming request and outgoing response.
 *
 * @since 1.0
 */
public interface HttpContext {

  /**
   * Upgrades this context to a websocket connection. In normal cases, as CloudNet currently only supports Http 1.X the
   * client sends an upgrade header in its request to signal that an upgrade to a websocket channel is requested. See
   * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Protocol_upgrade_mechanism">Mozilla - Upgrade</a> docs
   * for more information on how to use the header and the handshaking between the client and server works.
   * <p>
   * This method blocks the current thread until the handshake processes completed. Either successfully or with a
   * failure of any kind.
   * <p>
   * If the upgrade to a websocket connection is not possible, the method automatically answers with a reason text to
   * the client why the upgrade failed. Either because the socket version is not supported, or an exception occurred.
   * Supported web socket versions are:
   * <ol>
   *   <li>Wire protocol version 13 (RFC-6455/draft version 17 of the hybi specification)
   * </ol>
   * <p>
   * If the upgrade fails this method returns a task which is completed exceptionally and sends a response to the client
   * indicating the reason for the failure. After that, the connection is still able to send out http requests but will
   * no longer accept / handle these.
   *
   * @return a task completed with the upgraded web socket channel or an exception if the upgrade was not possible.
   */
  @NonNull CompletableFuture<WebSocketChannel> upgrade();

  /**
   * Gets the web socket channel to which this request was upgraded. If the request was not upgraded, or the upgrade was
   * not possible this method returns null.
   *
   * @return the upgraded web socket channel, or null if the connection upgrade was not done or failed.
   */
  @Nullable WebSocketChannel webSocketChannel();

  /**
   * Gets the channel to which the request came which is currently processed.
   *
   * @return the channel to which the request came.
   */
  @NonNull HttpChannel channel();

  /**
   * Gets the current request which is handled in this context.
   *
   * @return the current request.
   */
  @NonNull HttpRequest request();

  /**
   * Gets the current response which will be sent to the client.
   *
   * @return the current response.
   */
  @NonNull HttpResponse response();

  /**
   * Gets the http component which received the request wrapped by this context.
   *
   * @return the http component which received the request.
   */
  @NonNull HttpComponent<?> component();

  /**
   * Gets the extracted connection info about the client that requested the server information and the actual requested
   * server host. This information might for example be extracted from the Forwarded and Host HTTP header.
   * <p>
   * The final extraction of the information is done by using the resolver supplied by the http component that is
   * associated with this context. The extraction behaviour can therefore change and is therefor not necessarily
   * consistent.
   *
   * @return the extracted client connection info.
   */
  @NonNull BasicHttpConnectionInfo connectionInfo();

  /**
   * Sets whether the connection to client should be closed after the last handler in the chain. This defaults to false.
   * If set to false, the connection will not be closed and the {@code connection} header will automatically be set to
   * {@code keep-alive}. <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive">Mdn docs</a> are
   * providing a more in-depth walk through which other components may be set by a developer to customize the keep alive
   * header.
   *
   * @param value true if the connection should get closed after the last handler, false to keep it open.
   * @return the same instance of the context as used to call the method, for chaining.
   */
  @NonNull HttpContext closeAfter(boolean value);

  /**
   * Gets whether the connection to the client should be terminated after the last handler in the chain.
   *
   * @return true if the connection should be terminated, false otherwise.
   */
  boolean closeAfter();

  /**
   * Gets all invocation hints which are registered for the given key. Invocation hints are only valid for the current
   * handler call chain, and will be reset after a handler was called. Hints can for example get registered in a http
   * context preprocessor.
   * <p>
   * This method never returns null. If no hints are registered for the given key then an empty collection is returned.
   *
   * @param key the key of the hints to retrieve.
   * @return the registered hints for the given key, or an empty collection if no hints are registered.
   * @throws NullPointerException if the given key is null.
   */
  @NonNull Collection<Object> invocationHints(@NonNull String key);

  /**
   * Adds an invocation hint for the current handler call chain. Invocation hints are only valid for the current handler
   * call chain, and will be reset after a handler was called. Hints can for example get registered in a http context
   * preprocessor.
   * <p>
   * One key can be mapped to multiple hints.
   *
   * @param key   the key of the hint to register.
   * @param value the hint.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given key or hint is null.
   */
  @NonNull HttpContext addInvocationHint(@NonNull String key, @NonNull Object value);

  /**
   * Adds an invocation hint for the current handler call chain. Invocation hints are only valid for the current handler
   * call chain, and will be reset after a handler was called. Hints can for example get registered in a http context
   * preprocessor.
   * <p>
   * One key can be mapped to multiple hints.
   *
   * @param key   the key of the hint to register.
   * @param value the hints to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given key or hint is null.
   */
  @NonNull <T> HttpContext addInvocationHints(@NonNull String key, @NonNull Collection<T> value);
}
