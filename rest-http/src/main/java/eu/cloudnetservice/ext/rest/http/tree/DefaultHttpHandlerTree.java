package eu.cloudnetservice.ext.rest.http.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

@ApiStatus.Internal
public final class DefaultHttpHandlerTree implements HttpHandlerTree<HttpPathNode> {

  private static final HttpPathNode ROOT_PATH_NODE = new StaticHttpPathNode("/");
  private static final Comparator<HttpHandlerTree<HttpPathNode>> PATH_NODE_COMPARATOR =
    Comparator.comparing(HttpHandlerTree::pathNode);

  private HttpPathNode pathNode;

  private final DefaultHttpHandlerTree parentNode;
  private final List<DefaultHttpHandlerTree> children = new ArrayList<>(4);

  public static @NonNull DefaultHttpHandlerTree newTree() {
    return new DefaultHttpHandlerTree(ROOT_PATH_NODE, null);
  }

  private DefaultHttpHandlerTree(@NonNull HttpPathNode pathNode, @Nullable DefaultHttpHandlerTree parentNode) {
    this.pathNode = pathNode;
    this.parentNode = parentNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpPathNode pathNode() {
    return this.pathNode;
  }

  public void pathNode(@NonNull HttpPathNode pathNode) {
    this.pathNode = pathNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable DefaultHttpHandlerTree parentNode() {
    return this.parentNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnmodifiableView
  public @NonNull Collection<HttpHandlerTree<HttpPathNode>> children() {
    return Collections.unmodifiableCollection(this.children);
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
  public void visitFullTree(@NonNull Consumer<HttpHandlerTree<HttpPathNode>> nodeConsumer) {
    // visit this node first
    nodeConsumer.accept(this);

    // visit all child nodes
    DefaultHttpHandlerTree currentChild;
    Queue<DefaultHttpHandlerTree> nodesToVisit = new ArrayDeque<>(this.children);
    while ((currentChild = nodesToVisit.poll()) != null) {
      nodeConsumer.accept(currentChild);
      nodesToVisit.addAll(currentChild.children);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable DefaultHttpHandlerTree findMatchingChildNode(
    @NotNull Predicate<HttpHandlerTree<HttpPathNode>> nodeFilter
  ) {
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
  public @NonNull DefaultHttpHandlerTree registerChildNode(@NonNull HttpPathNode pathNode) {
    // return the currently registered node if any is already registered
    var registeredChildNode = this.findMatchingChildNode(node -> node.pathNode().equals(pathNode));
    if (registeredChildNode != null) {
      return registeredChildNode;
    }

    // register & return the new tree node
    var childNode = new DefaultHttpHandlerTree(pathNode, this);
    this.children.add(childNode);
    this.children.sort(PATH_NODE_COMPARATOR);
    return childNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregisterChildNode(@NonNull HttpHandlerTree<HttpPathNode> node) {
    //noinspection SuspiciousMethodCalls
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
    if (!(o instanceof DefaultHttpHandlerTree that)) {
      return false;
    }
    return this.pathNode.equals(that.pathNode);
  }
}
