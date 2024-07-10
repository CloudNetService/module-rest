/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.modules.rest.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.ext.modules.rest.auth.DefaultRestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class RestUserUpdateListener {

  @EventListener
  private void handleRestUserUpdate(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull RestUserManagement restUserManagement
  ) {
    if (event.channel().equals(DefaultRestUserManagement.REST_USER_MANAGEMENT_CHANNEL)
      && event.message().equals(DefaultRestUserManagement.REST_USER_INVALIDATE)) {
      if (restUserManagement instanceof DefaultRestUserManagement defaultRestUserManagement) {
        defaultRestUserManagement.invalidate(event.content().readUniqueId());
      }
    }
  }
}
