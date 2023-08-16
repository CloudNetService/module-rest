/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface HttpPathNode extends Comparable<HttpPathNode> {

  static @NonNull HttpPathNode root() {
    return StaticHttpPathNode.ROOT_PATH_NODE;
  }

  static void validatePathId(@NonNull String candidate) {
    if (candidate.isBlank()) {
      throw new IllegalArgumentException("Empty path parts are not allowed");
    }
  }

  boolean consumesRemainingPath();

  @NonNull String displayName();

  /**
   * Get the path entry that is represented by this node.
   *
   * @return the id of this path node.
   */
  @NonNull String pathId();

  boolean anyHandlerRegistered();

  @NonNull List<HttpHandlerConfigPair> handlers();

  @Nullable HttpHandlerConfigPair findHandlerForMethod(@NonNull String method);

  void registerHttpHandler(@NonNull HttpHandler httpHandler, @NonNull HttpHandlerConfig config);

  boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart);
}