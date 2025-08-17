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

import com.google.common.base.Splitter;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.tree.DynamicHttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.HttpHandlerTree;
import eu.cloudnetservice.ext.rest.api.tree.HttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.StaticHttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.WildcardPathNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The default implementation of the http handler registry.
 *
 * @since 1.0
 */
final class DefaultHttpHandlerRegistry implements HttpHandlerRegistry {

  private static final Splitter PATH_PARTS_SPLITTER = Splitter.on('/');
  private static final Pattern DYNAMIC_NODE_PATTERN =
    Pattern.compile("^\\{\\s*(?:<[^>]+>\\s*)?([^}]+)\\s*}(?:\\s*;(.+?)(?:;([123imsug]*))?$)?$");

  private static final Predicate<HttpHandlerTree<HttpPathNode>> DYNAMIC_PATH_NODE_FILTER =
    node -> node.pathNode() instanceof DynamicHttpPathNode;
  private static final Predicate<HttpHandlerTree<HttpPathNode>> CONSUMES_EVERYTHING_NODE_FILTER =
    node -> node.pathNode().consumesRemainingPath();

  private final ComponentConfig componentConfig;
  private final HttpHandlerTree<HttpPathNode> rootHandlerTreeNode;

  /**
   * Constructs a new http handler registry with the given component config to configure the registry.
   *
   * @param componentConfig the component config used to configure the registry.
   * @throws NullPointerException if the given component config is null.
   */
  public DefaultHttpHandlerRegistry(@NonNull ComponentConfig componentConfig) {
    this.componentConfig = componentConfig;
    this.rootHandlerTreeNode = HttpHandlerTree.newHandlerTree(HttpPathNode.newRootNode());
  }

  /**
   * Removes a leading and trailing slash from the given path.
   *
   * @param path the path to strip the slashes from.
   * @return the path with stripped slashes.
   * @throws NullPointerException if the given path is null.
   */
  private static @NonNull String removeSlashPrefixSuffixFromPath(@NonNull String path) {
    var prefixedWithSlash = path.startsWith("/");
    var suffixedWithSlash = (!prefixedWithSlash || path.length() > 1) && path.endsWith("/");
    if (prefixedWithSlash || suffixedWithSlash) {
      path = path.substring(prefixedWithSlash ? 1 : 0, suffixedWithSlash ? path.length() - 1 : path.length());
    }
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Collection<HttpHandler> registeredHandlers() {
    List<HttpHandler> httpHandlers = new ArrayList<>();
    this.rootHandlerTreeNode.visitFullTree(node -> {
      var handlerPairs = node.pathNode().handlers();
      if (!handlerPairs.isEmpty()) {
        for (var handler : handlerPairs) {
          httpHandlers.add(handler.httpHandler());
        }
      }
    });
    return httpHandlers;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpHandlerTree<HttpPathNode> findHandler(@NonNull String path, @NonNull HttpContext context) {
    // remove the / prefix and/or suffix from the given input path
    path = removeSlashPrefixSuffixFromPath(path);

    // check if the root handler was requested
    if (path.isBlank() || path.equals("/")) {
      return this.rootHandlerTreeNode;
    }

    // basically filter for two things:
    //  1. a fully matching node for the given request
    //  2. a wildcard node that is located the deepest in the tree path
    HttpHandlerTree<HttpPathNode> lastConsumingNode = null;
    HttpHandlerTree<HttpPathNode> bestMatch = this.rootHandlerTreeNode;

    // a stack of the nodes that were visited since the last node that consumes the rest of the path
    // null in case no such node was encountered before
    // this uses a Deque rather than a java.util.Stack as advised in the Stack javadocs
    Deque<HttpHandlerTree<HttpPathNode>> visitedNodesSinceLastConsumingNode = null;

    // find the best matching node for the given path based on the supplied parts
    var pathParts = PATH_PARTS_SPLITTER.split(path);
    for (var pathPart : pathParts) {
      // find a node that consumes the full path on the current best match
      var consumingNode = bestMatch.findMatchingDirectChild(CONSUMES_EVERYTHING_NODE_FILTER);
      if (consumingNode != null) {
        lastConsumingNode = consumingNode;
        visitedNodesSinceLastConsumingNode = new ArrayDeque<>(5);
      }

      // find a matching sub node or break in case no node is matching
      bestMatch = bestMatch.findMatchingDirectChild(n -> n.pathNode().validateAndRegisterPathPart(context, pathPart));
      if (bestMatch == null || CONSUMES_EVERYTHING_NODE_FILTER.test(bestMatch)) {
        break;
      }

      // push the matching node as visited in the current stack
      // in case we found a node that consumes the full path before
      if (visitedNodesSinceLastConsumingNode != null) {
        visitedNodesSinceLastConsumingNode.addFirst(bestMatch);
      }
    }

    // return the best matching node in case we found one
    if (bestMatch != null) {
      return bestMatch;
    }

    // rollback the changes made to the handling context that were caused by nodes after the last consuming node
    if (visitedNodesSinceLastConsumingNode != null) {
      HttpHandlerTree<HttpPathNode> treeEntry;
      while ((treeEntry = visitedNodesSinceLastConsumingNode.pollFirst()) != null) {
        treeEntry.pathNode().unregisterPathPart(context);
      }
    }

    // returns either the last consuming node or null in case we found no such node
    return lastConsumingNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerHandler(@NonNull String path, @NonNull HttpHandler handler, @NonNull HttpHandlerConfig config) {
    // no need to do further checks if the root handler was requested
    var targetTreeNode = this.rootHandlerTreeNode;
    if (!path.isBlank() && !path.equals("/")) {
      // remove the / prefix and/or suffix from the given input path
      path = removeSlashPrefixSuffixFromPath(path);

      // split the path into separate parts after each slash
      var pathParts = PATH_PARTS_SPLITTER.splitToList(path);
      var lastPathPartIndex = pathParts.size() - 1;
      for (int partIndex = 0; partIndex < pathParts.size(); partIndex++) {
        var pathPart = pathParts.get(partIndex);

        // check if the node is a dynamic node
        if (pathPart.startsWith("{")) {
          var dynamicNodeMatcher = DYNAMIC_NODE_PATTERN.matcher(pathPart);
          if (!dynamicNodeMatcher.matches()) {
            throw new HttpHandlerRegisterException(
              "Tried to register dynamic node with invalid declaration: %s -> [%s]",
              targetTreeNode.treePath(), pathPart);
          }

          var pathId = dynamicNodeMatcher.group(1);
          HttpPathNode.validatePathId(pathId);

          // check if there is a dynamic node somewhere up the tree with the same name already
          var existingDynamicNode = targetTreeNode.findMatchingParent(
            DYNAMIC_PATH_NODE_FILTER.and(node -> node.pathNode().pathId().equals(pathId)));
          if (existingDynamicNode != null) {
            throw new HttpHandlerRegisterException(
              "Tried to register dynamic node with same name '%s' as already registered in the path: %s -> [%s]",
              pathId, targetTreeNode.treePath(), pathPart);
          }

          // ensure that there is only one dynamic node at the same handling level
          var existingDirectDynamicNode = targetTreeNode.findMatchingDirectChild(
            DYNAMIC_PATH_NODE_FILTER.and(node -> !node.pathNode().pathId().equals(pathId)));
          if (existingDirectDynamicNode != null) {
            throw new HttpHandlerRegisterException(
              "Tried to register dynamic node '%s' alongside dynamic node '%s' at same path level: %s",
              pathId, existingDirectDynamicNode.pathNode().pathId(), targetTreeNode.treePath());
          }

          // register or re-use the existing child node
          var node = DynamicHttpPathNode.parse(pathId, dynamicNodeMatcher.group(2), dynamicNodeMatcher.group(3));
          targetTreeNode = targetTreeNode.registerChildNode(node);
          continue;
        }

        // check for a wildcard node
        if (pathPart.equals("*")) {
          // validate that the wildcard is not in the middle of the path
          if (partIndex != lastPathPartIndex) {
            throw new HttpHandlerRegisterException(
              "Tried to register dynamic node in the middle of the handler uri '%s' while only allowed at the end",
              path);
          }

          // register the wildcard node and break (there are no further parts following as validated before)
          var node = new WildcardPathNode();
          targetTreeNode = targetTreeNode.registerChildNode(node);
          break;
        }

        // must be a static path node
        HttpPathNode.validatePathId(pathPart);
        var node = new StaticHttpPathNode(pathPart);
        targetTreeNode = targetTreeNode.registerChildNode(node);
      }
    }

    // ensure that there are not two handlers for the same http method on the same path
    var targetPathNode = targetTreeNode.pathNode();
    var existingHandler = targetPathNode.findHandlerForMethod(config.httpMethod().name());
    if (existingHandler != null) {
      throw new HttpHandlerRegisterException(
        "Tried to register second http handler for method %s for path: %s",
        config.httpMethod(), targetTreeNode.treePath());
    }

    // construct the final handler config & register the http handler
    var mergedCorsConfig = this.componentConfig.corsConfig().combine(config.corsConfig());
    var handlerConfig = HttpHandlerConfig.builder(config).corsConfiguration(mergedCorsConfig).build();
    targetPathNode.registerHttpHandler(handler, handlerConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterHandler(@NonNull HttpHandler handler) {
    this.rootHandlerTreeNode.visitFullTree(treeNode -> {
      var removedAnyHandler = treeNode.pathNode().unregisterHttpHandler(handler);
      if (removedAnyHandler) {
        this.unregisterTreeNodesWithoutHandler(treeNode);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterHandlers(@NonNull ClassLoader classLoader) {
    this.rootHandlerTreeNode.visitFullTree(treeNode -> {
      var removedAnyHandler = treeNode.pathNode()
        .unregisterMatchingHandler(pair -> pair.httpHandler().getClass().getClassLoader() == classLoader);
      if (removedAnyHandler) {
        this.unregisterTreeNodesWithoutHandler(treeNode);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearHandlers() {
    this.rootHandlerTreeNode.removeAllChildren();
  }

  private void unregisterTreeNodesWithoutHandler(@NonNull HttpHandlerTree<HttpPathNode> treeNode) {
    if (treeNode.pathNode().handlerCount() == 0 && treeNode.childCount() == 0) {
      var currentParent = treeNode.parentNode();
      if (currentParent != null) {
        do {
          if (currentParent.root()
            || currentParent.childCount() > 1
            || currentParent.pathNode().handlerCount() > 0
          ) {
            // 2 cases that can happen:
            //   1. parent node is the root node - we cannot unregister anything above that,
            //      so we just unregister the current tree node from the parent (root) node.
            //   2. the parent has registered handlers / child nodes, so we remove the current
            //      tree node from that parent node to leave the node with handlers in the tree.
            currentParent.unregisterChildNode(treeNode);
            break;
          }

          // try the next tree entry: move the current node we're checking to the current parent and get the next parent
          // node. due to the root check before, we should never hit a state where the next parent is null.
          treeNode = currentParent;
          currentParent = currentParent.parentNode();
        } while (currentParent != null);
      }
    }
  }
}
