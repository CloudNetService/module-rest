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

package eu.cloudnetservice.ext.modules.rest;

import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.ext.modules.rest.config.RestConfiguration;
import eu.cloudnetservice.ext.modules.rest.listener.CloudNetBridgeInitializer;
import eu.cloudnetservice.ext.modules.rest.listener.RestUserUpdateListener;
import eu.cloudnetservice.ext.modules.rest.processor.CloudNetLoggerInterceptor;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerAuthorization;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerCluster;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerDatabase;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerDocumentation;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerGroup;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerModule;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerNode;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerService;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerServiceVersion;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerTask;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerTemplate;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerTemplateStorage;
import eu.cloudnetservice.ext.modules.rest.v3.V3HttpHandlerUser;
import eu.cloudnetservice.ext.rest.api.HttpServer;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import eu.cloudnetservice.ext.rest.api.factory.HttpComponentFactoryLoader;
import eu.cloudnetservice.ext.rest.validation.ValidationHandlerMethodContextDecorator;
import eu.cloudnetservice.node.command.CommandProvider;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CloudNetRestModule extends DriverModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetRestModule.class);

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void loadLanguageFile() {
    I18n.loadFromLangPath(this.getClass());
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
  public void initHttpServer(@NonNull InjectionLayer<?> injectionLayer) {
    var restConfig = this.readConfig(RestConfiguration.class, () -> RestConfiguration.DEFAULT, DocumentFactory.json());

    // construct the http server component
    var componentFactory = HttpComponentFactoryLoader.getFirstComponentFactory(HttpServer.class);
    var server = componentFactory.construct(restConfig.toComponentConfig());

    // registers the validation-enabling context decorator
    var validationDecorator = ValidationHandlerMethodContextDecorator.withDefaultValidator();
    server.annotationParser().registerHandlerContextDecorator(validationDecorator);

    // bind the server and register it for injection
    restConfig.httpListeners().forEach(listener -> server.addListener(listener).join());
    injectionLayer.install(BindingBuilder.create().bind(HttpServer.class).toInstance(server));

    // add the cloudnet logger interceptor
    server.annotationParser().registerAnnotationProcessor(new CloudNetLoggerInterceptor());

    // bind the rest user management for injection
    var restUserManagement = RestUserManagementLoader.load();
    injectionLayer.install(BindingBuilder.create().bind(RestUserManagement.class).toInstance(restUserManagement));
  }

  @ModuleTask(order = 107, lifecycle = ModuleLifeCycle.STARTED)
  public void registerHttpHandlers(
    @NonNull HttpServer httpServer,
    @NonNull V3HttpHandlerUser userHandler,
    @NonNull V3HttpHandlerTask taskHandler,
    @NonNull V3HttpHandlerNode nodeHandler,
    @NonNull V3HttpHandlerGroup groupHandler,
    @NonNull V3HttpHandlerModule moduleHandler,
    @NonNull V3HttpHandlerCluster clusterHandler,
    @NonNull V3HttpHandlerService serviceHandler,
    @NonNull V3HttpHandlerDatabase databaseHandler,
    @NonNull V3HttpHandlerTemplate templateHandler,
    @NonNull V3HttpHandlerServiceVersion versionHandler,
    @NonNull V3HttpHandlerTemplateStorage storageHandler,
    @NonNull V3HttpHandlerAuthorization authorizationHandler,
    @NonNull V3HttpHandlerDocumentation documentationHandler
  ) {
    httpServer.annotationParser()
      .parseAndRegister(userHandler)
      .parseAndRegister(taskHandler)
      .parseAndRegister(nodeHandler)
      .parseAndRegister(groupHandler)
      .parseAndRegister(moduleHandler)
      .parseAndRegister(clusterHandler)
      .parseAndRegister(serviceHandler)
      .parseAndRegister(databaseHandler)
      .parseAndRegister(templateHandler)
      .parseAndRegister(versionHandler)
      .parseAndRegister(storageHandler)
      .parseAndRegister(authorizationHandler)
      .parseAndRegister(documentationHandler);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  public void registerRestCommand(@NonNull CommandProvider commandProvider) {
    commandProvider.register(RestCommand.class);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  public void registerListener(@NonNull EventManager eventManager) {
    eventManager.registerListener(RestUserUpdateListener.class);
    eventManager.registerListener(CloudNetBridgeInitializer.class);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  public void unregisterModule(@NonNull HttpServer httpServer) {
    try {
      httpServer.close();
    } catch (Exception exception) {
      LOGGER.error("Unable to close http server while disabling cloudnet rest module.", exception);
    }
  }
}
