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

package eu.cloudnetservice.ext.rest.api.tree;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * DO NOT STORE A REFERENCE!!!!
 */
public interface HttpHandlerTree<N extends HttpPathNode> {

  @Contract("_ -> new")
  static @NonNull <N extends HttpPathNode> HttpHandlerTree<N> newHandlerTree(@NonNull N rootPathNode) {
    return new DefaultHttpHandlerTree<>(rootPathNode, null);
  }

  boolean root();

  int childCount();

  @NonNull String treePath();

  @NonNull N pathNode();

  @Nullable HttpHandlerTree<N> parentNode();

  @NonNull Collection<HttpHandlerTree<N>> children();

  void removeAllChildren();

  void visitFullTree(@NonNull Consumer<HttpHandlerTree<N>> nodeConsumer);

  @Nullable HttpHandlerTree<N> findMatchingParent(@NonNull Predicate<HttpHandlerTree<N>> nodeFilter);

  /**
   * Performs a first level search for a node matching the given filter. Returns null in case no direct child matches
   * the given filter.
   *
   * @param nodeFilter the filter.
   * @return the first node matching the given filter or null in case no child node matches.
   */
  @Nullable HttpHandlerTree<N> findMatchingDirectChild(@NonNull Predicate<HttpHandlerTree<N>> nodeFilter);

  @NonNull HttpHandlerTree<N> registerChildNode(@NonNull N pathNode);

  boolean unregisterChildNode(@NonNull HttpHandlerTree<N> node);
}
