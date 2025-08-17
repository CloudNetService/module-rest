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

import eu.cloudnetservice.ext.rest.api.HttpChannel;
import eu.cloudnetservice.ext.rest.api.util.HostAndPort;
import io.netty5.channel.Channel;
import lombok.NonNull;

/**
 * The default implementation of the http channel, delegating all method calls to netty.
 *
 * @since 1.0
 */
record NettyHttpChannel(
  @NonNull Channel channel,
  @NonNull String scheme,
  @NonNull HostAndPort serverAddress,
  @NonNull HostAndPort clientAddress
) implements HttpChannel {

  @Override
  public void close() {
    this.channel.close();
  }
}
