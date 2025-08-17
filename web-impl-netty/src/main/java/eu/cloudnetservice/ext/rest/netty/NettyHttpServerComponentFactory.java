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

import eu.cloudnetservice.ext.rest.api.HttpServer;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.factory.HttpComponentFactory;
import lombok.NonNull;

public final class NettyHttpServerComponentFactory implements HttpComponentFactory<HttpServer> {

  @Override
  public @NonNull String componentTypeName() {
    return "Netty Http-Server";
  }

  @Override
  public @NonNull Class<HttpServer> supportedComponentType() {
    return HttpServer.class;
  }

  @Override
  public @NonNull HttpServer construct(@NonNull ComponentConfig componentConfig) {
    return new NettyHttpServer(componentConfig);
  }
}
