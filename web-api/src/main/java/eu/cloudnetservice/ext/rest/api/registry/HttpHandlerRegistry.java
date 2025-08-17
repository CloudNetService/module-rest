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

package eu.cloudnetservice.ext.rest.api.registry;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.tree.HttpHandlerTree;
import eu.cloudnetservice.ext.rest.api.tree.HttpPathNode;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface HttpHandlerRegistry {

  /**
   * Constructs a new http handler registry with the given component config to configure the registry.
   *
   * @param componentConfig the component config used to configure the registry.
   * @return the newly created http handler registry.
   * @throws NullPointerException if the given component config is null.
   */
  static @NonNull HttpHandlerRegistry newHandlerRegistry(@NonNull ComponentConfig componentConfig) {
    return new DefaultHttpHandlerRegistry(componentConfig);
  }

  /**
   * Gets all registered handler in this handler registry.
   * <p>
   * Modifications to the given collection are not possible and will result in an
   * {@link UnsupportedOperationException}.
   *
   * @return all registered handler in this handler registry.
   */
  @Unmodifiable
  @NonNull Collection<HttpHandler> registeredHandlers();

  /**
   * Searches for a http handler in the handler tree that matches the given request path.
   *
   * @param path    the path the handler is bound to.
   * @param context the request context to search with.
   * @return a http handler in the handler tree that matches the given path, null if no http handler matches.
   * @throws NullPointerException if the given path or context is null.
   */
  @Nullable HttpHandlerTree<HttpPathNode> findHandler(@NonNull String path, @NonNull HttpContext context);

  /**
   * Registers a new http handler to this handler registry. The handler registry supports three kinds of nodes:
   * <ul>
   *   <li>A static node, only accepting the literal path {@code api/v2/players}</li>
   *   <li>A dynamic node, accepting a path parameter {@code api/v2/player/{name}}</li>
   *   <li>A wildcard node, matching everything after the asterix {@code api/v2/help/*}</li>
   * </ul>
   * Rules for wildcard nodes:
   * <ul>
   *   <li>A wildcard can only be used at the <strong>END</strong> of a path and not in the middle.</li>
   * </ul>
   * <p>
   * Rules for dynamic nodes:
   * <ul>
   *   <li>A path is not allowed to have multiple dynamic nodes with the same node.
   *   While {@code api/v2/player/{name}/connect/{server}} is allowed,
   *   {@code api/v2/player/{name}/connect/{name}} is <strong>NOT</strong> allowed.</li>
   *   <li>Different handlers must have the same dynamic node on the same level.
   *   While {@code api/v2/{name}/kick} and {@code api/v2/{name}/connect} is allowed,
   *   {@code api/v2/{name}/kick} and {@code api/v2/{player}/connect} is <strong>NOT</strong> allowed.</li>
   * </ul>
   *
   * @param path    the path to register the handler for.
   * @param handler the handler to register.
   * @param config  the config to the handler.
   * @throws HttpHandlerRegisterException if any of the mentioned rules is violated.
   * @throws NullPointerException         if the given path, handler or config is null.
   */
  void registerHandler(
    @NonNull String path,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config);

  /**
   * Unregisters the given handler from the handler registry and removes the now empty nodes from the tree.
   *
   * @param handler the handler to unregister.
   * @throws NullPointerException if the given handler is null.
   */
  void unregisterHandler(@NonNull HttpHandler handler);

  /**
   * Unregisters all http handlers that were loaded from the given class loader  and removes the now empty nodes from
   * the tree.
   *
   * @param classLoader the class loader the handler were loaded from.
   * @throws NullPointerException if the given class loader is null.
   */
  void unregisterHandlers(@NonNull ClassLoader classLoader);

  /**
   * Removes all handlers from this registry.
   */
  void clearHandlers();
}
