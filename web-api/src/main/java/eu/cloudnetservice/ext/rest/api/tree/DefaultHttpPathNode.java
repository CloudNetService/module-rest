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

import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

abstract sealed class DefaultHttpPathNode
  implements HttpPathNode
  permits DynamicHttpPathNode, StaticHttpPathNode, WildcardPathNode {

  protected final String pathId;
  protected final List<HttpHandlerConfigPair> handlers;

  DefaultHttpPathNode(@NonNull String pathId) {
    this.pathId = pathId;
    this.handlers = new ArrayList<>();
  }

  @Override
  public boolean consumesRemainingPath() {
    return false;
  }

  @Override
  public @NonNull String pathId() {
    return this.pathId;
  }

  @Override
  public int handlerCount() {
    return this.handlers.size();
  }

  @Override
  public @NonNull List<HttpHandlerConfigPair> handlers() {
    return this.handlers;
  }

  @Override
  public @Nullable HttpHandlerConfigPair findHandlerForMethod(@NonNull String method) {
    return this.handlers.stream()
      .filter(pair -> pair.config().httpMethod().name().equalsIgnoreCase(method))
      .findFirst()
      .orElse(null);
  }

  @Override
  public void registerHttpHandler(@NonNull HttpHandler httpHandler, @NonNull HttpHandlerConfig config) {
    this.handlers.add(new HttpHandlerConfigPair(httpHandler, config));
  }

  @Override
  public boolean unregisterHttpHandler(@NonNull HttpHandler httpHandler) {
    return this.unregisterMatchingHandler(pair -> pair.httpHandler() == httpHandler);
  }

  @Override
  public boolean unregisterMatchingHandler(@NonNull Predicate<HttpHandlerConfigPair> filter) {
    return this.handlers.removeIf(filter);
  }
}
