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

import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.ext.modules.rest.v3.bridge.V3HttpHandlerPlayer;
import eu.cloudnetservice.ext.rest.api.HttpServer;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudNetBridgeInitializer {

  private static final String BRIDGE_MODULE_NAME = "CloudNet-Bridge";

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetBridgeInitializer.class);

  public static void installBridgeHandler(
    @NonNull ModuleProvider moduleProvider,
    @NonNull HttpServer server,
    @NonNull InjectionLayer<SpecifiedInjector> moduleLayer
  ) {
    var bridgeModule = moduleProvider.module(BRIDGE_MODULE_NAME);
    if (bridgeModule == null || bridgeModule.moduleLifeCycle() != ModuleLifeCycle.STARTED) {
      LOGGER.debug("Could not detect started {} module. Aborting http player initialization.", BRIDGE_MODULE_NAME);
      return;
    }

    server.annotationParser().parseAndRegister(moduleLayer.instance(V3HttpHandlerPlayer.class));
    LOGGER.debug("Successfully registered V3HttpHandlerPlayer as {} is present", BRIDGE_MODULE_NAME);
  }
}
