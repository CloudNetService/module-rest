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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class DefaultHttpHandlerTree<N extends HttpPathNode> implements HttpHandlerTree<N> {

  private static final Comparator<HttpHandlerTree<?>> PATH_NODE_COMPARATOR =
    Comparator.comparing(HttpHandlerTree::pathNode);

  private final N pathNode;
  private final HttpHandlerTree<N> parentNode;
  private final List<HttpHandlerTree<N>> children = new ArrayList<>();

  public DefaultHttpHandlerTree(@NonNull N pathNode, @Nullable HttpHandlerTree<N> parentNode) {
    this.pathNode = pathNode;
    this.parentNode = parentNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean root() {
    return this.parentNode == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int childCount() {
    return this.children.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String treePath() {
    Deque<String> pathEntries = new ArrayDeque<>();

    // visit all path nodes from this node upwards in the tree
    HttpHandlerTree<N> handlerTree = this;
    do {
      pathEntries.offerFirst(handlerTree.pathNode().displayName());
    } while ((handlerTree = handlerTree.parentNode()) != null);

    // convert the path to a string
    return String.join(" -> ", pathEntries);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull N pathNode() {
    return this.pathNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpHandlerTree<N> parentNode() {
    return this.parentNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<HttpHandlerTree<N>> children() {
    return this.children;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAllChildren() {
    this.children.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitFullTree(@NonNull Consumer<HttpHandlerTree<N>> nodeConsumer) {
    // visit this node first
    nodeConsumer.accept(this);

    // visit all child nodes
    HttpHandlerTree<N> currentChild;
    Queue<HttpHandlerTree<N>> nodesToVisit = new ArrayDeque<>(this.children);
    while ((currentChild = nodesToVisit.poll()) != null) {
      nodeConsumer.accept(currentChild);
      nodesToVisit.addAll(currentChild.children());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpHandlerTree<N> findMatchingParent(@NonNull Predicate<HttpHandlerTree<N>> nodeFilter) {
    HttpHandlerTree<N> handlerTree = this;
    do {
      if (nodeFilter.test(handlerTree)) {
        return handlerTree;
      }
    } while ((handlerTree = handlerTree.parentNode()) != null);
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpHandlerTree<N> findMatchingDirectChild(@NonNull Predicate<HttpHandlerTree<N>> nodeFilter) {
    for (var child : this.children) {
      if (nodeFilter.test(child)) {
        return child;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpHandlerTree<N> registerChildNode(@NonNull N pathNode) {
    // return the currently registered node if any is already registered
    var registeredChildNode = this.findMatchingDirectChild(node -> {
      var registeredNode = node.pathNode();
      return registeredNode.pathId().equals(pathNode.pathId()) && registeredNode.getClass().equals(pathNode.getClass());
    });
    if (registeredChildNode != null) {
      return registeredChildNode;
    }

    // register & return the new tree node
    var childNode = new DefaultHttpHandlerTree<>(pathNode, this);
    this.children.add(childNode);
    this.children.sort(PATH_NODE_COMPARATOR);
    return childNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregisterChildNode(@NonNull HttpHandlerTree<N> node) {
    return this.children.remove(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HttpHandlerTree<?> that)) {
      return false;
    }
    return this.pathNode.equals(that.pathNode());
  }
}
