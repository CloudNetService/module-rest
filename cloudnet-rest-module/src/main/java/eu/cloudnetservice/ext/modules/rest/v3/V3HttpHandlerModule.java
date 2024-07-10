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

package eu.cloudnetservice.ext.modules.rest.v3;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestBody;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.updater.util.ChecksumUtil;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.module.ModuleEntry;
import eu.cloudnetservice.node.module.ModulesHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class V3HttpHandlerModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(V3HttpHandlerModule.class);

  private final ModulesHolder modulesHolder;
  private final ModuleProvider moduleProvider;

  @Inject
  public V3HttpHandlerModule(@NonNull ModulesHolder modulesHolder, @NonNull ModuleProvider moduleProvider) {
    this.modulesHolder = modulesHolder;
    this.moduleProvider = moduleProvider;
  }

  @RequestHandler(path = "/api/v3/module/reload", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_reload_all"})
  public @NonNull IntoResponse<?> handleModuleReloadRequest() {
    this.moduleProvider.reloadAll();
    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/module/loaded")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_list_loaded"})
  public @NonNull IntoResponse<?> handleModuleLoadedListRequest() {
    var modules = this.moduleProvider.modules().stream().map(this::constructModuleInformation).toList();
    return JsonResponse.builder().body(Map.of("modules", modules));
  }

  @RequestHandler(path = "/api/v3/module/present")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_list_present"})
  public @NonNull IntoResponse<?> handleModulePresentListRequest() {
    List<String> fileNames = new ArrayList<>();
    FileUtil.walkFileTree(
      this.moduleProvider.moduleDirectoryPath(),
      (__, current) -> fileNames.add(current.getFileName().toString()),
      false,
      "*.{jar,war}");
    return JsonResponse.builder().body(Map.of("modules", fileNames));
  }

  @RequestHandler(path = "/api/v3/module/available")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_list_available"})
  public @NonNull IntoResponse<?> handleModuleInstalledListRequest() {
    var modules = this.modulesHolder.entries().stream().peek(ModuleEntry::url).toList();
    return JsonResponse.builder().body(Map.of("modules", modules));
  }

  @RequestHandler(path = "/api/v3/module/{name}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_get"})
  public @NonNull IntoResponse<?> handleModuleGetRequest(@NonNull @RequestPathParam("name") String name) {
    return this.handleModuleContext(
      name,
      module -> JsonResponse.builder().body(this.constructModuleInformation(module)));
  }

  @RequestHandler(path = "/api/v3/module/{name}/lifecycle", method = HttpMethod.PATCH)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_lifecycle"})
  public @NonNull IntoResponse<?> handleModuleLifecycleRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("target") String lifecycle
  ) {
    return this.handleModuleContext(name, module -> {
      switch (StringUtil.toLower(lifecycle)) {
        case "start" -> module.startModule();
        case "reload" -> module.reloadModule();
        case "stop" -> module.stopModule();
        case "unload" -> module.unloadModule();
        default -> {
          return ProblemDetail.builder()
            .type("invalid-module-lifecycle")
            .title("Invalid Module Lifecycle")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail(String.format(
              "The requested module lifecycle %s does not exist. Supported values are: start, stop, reload, unload",
              lifecycle
            ));
        }
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  @RequestHandler(path = "/api/v3/module/{name}/uninstall", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_uninstall"})
  public @NonNull IntoResponse<?> handleModuleUninstallRequest(@NonNull @RequestPathParam("name") String name) {
    return this.handleModuleContext(name, module -> {
      module.stopModule();
      module.unloadModule();

      try {
        Files.delete(Path.of(module.uri()));
      } catch (IOException exception) {
        return ProblemDetail.builder()
          .type("module-uninstall-failed")
          .title("Module Uninstall Failed")
          .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
          .detail(String.format("Uninstalling module %s failed due to an internal I/O error.", name));
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  @RequestHandler(path = "/api/v3/module/{name}/load", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_load"})
  public @NonNull IntoResponse<?> handleModuleLoadRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestBody InputStream body
  ) {
    var moduleTarget = this.moduleProvider.moduleDirectoryPath().resolve(name);
    FileUtil.ensureChild(this.moduleProvider.moduleDirectoryPath(), moduleTarget);

    // we are doing this check so that you can load modules that are already available,
    // therefore the input stream might be empty
    try {
      // we want to read the first byte
      body.mark(1);
      // read the first byte, if its -1 the stream is empty
      var empty = body.read() == -1;
      // reset to the beginning
      body.reset();
      if (!empty) {
        try (var outputStream = Files.newOutputStream(moduleTarget)) {
          FileUtil.copy(body, outputStream);
        }
      }
    } catch (IOException exception) {
      LOGGER.debug("Exception handling module load request", exception);
      FileUtil.delete(moduleTarget);
      return ProblemDetail.builder()
        .type("module-load-failed-io")
        .title("Module Load Failed I/O")
        .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
        .detail(String.format("Loading module %s failed due to internal I/O error.", name));
    }

    var module = this.moduleProvider.loadModule(moduleTarget);
    if (module == null) {
      return ProblemDetail.builder()
        .type("module-load-failed")
        .title("Module Load Failed")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail(String.format("Loading module %s failed. The module is already loaded.", name));
    }

    return JsonResponse.builder().responseCode(HttpResponseCode.CREATED).body(this.constructModuleInformation(module));
  }

  @RequestHandler(path = "/api/v3/module/{name}/install", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_install"})
  public @NonNull IntoResponse<?> handleModuleInstallRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @Optional @FirstRequestQueryParam(value = "checksumValidation", def = "true") String validation,
    @NonNull @Optional @FirstRequestQueryParam(value = "start", def = "true") String start
  ) {
    var entry = this.modulesHolder.findByName(name).orElse(null);
    if (entry == null) {
      return ProblemDetail.builder()
        .type("module-not-found")
        .title("Module Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested module %s was not found.", name));
    }

    var dependencies = entry.dependingModules().stream()
      .filter(dependency -> this.moduleProvider.module(dependency) == null)
      .collect(Collectors.toSet());
    if (!dependencies.isEmpty()) {
      return ProblemDetail.builder();
    }

    // download the module
    var target = this.moduleProvider.moduleDirectoryPath().resolve(entry.name() + ".jar");
    Unirest.get(entry.url())
      .connectTimeout(5000)
      .thenConsume(rawResponse -> FileUtil.copy(rawResponse.getContent(), target));

    // validate the downloaded file
    var checksum = ChecksumUtil.fileShaSum(target);
    var checksumValidation = Boolean.parseBoolean(validation);
    if (!Node.DEV_MODE && !checksum.equals(entry.sha3256())) {
      // the checksum validation skip is only available for official modules
      if (!entry.official() || checksumValidation) {
        // remove the suspicious file
        FileUtil.delete(target);
        return ProblemDetail.builder()
          .status(HttpResponseCode.BAD_REQUEST)
          .type("module-install-checksum-failed")
          .title("Module Install Checksum Failed")
          .detail(String.format(
            "The checksum validation for the requested module %s failed."
              + " If the module is official you can skip using the checksumValidation query parameter.",
            name
          ));
      }
    }

    // load the module
    var wrapper = this.moduleProvider.loadModule(target);
    if (wrapper == null) {
      return ProblemDetail.builder()
        .type("module-load-failed")
        .title("Module Load Failed")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail(String.format("Loading module %s failed. The module is already loaded.", name));
    }

    if (Boolean.parseBoolean(start)) {
      wrapper.startModule();
    }

    return JsonResponse.builder().responseCode(HttpResponseCode.CREATED).body(this.constructModuleInformation(wrapper));
  }

  @RequestHandler(path = "/api/v3/module/{name}/config")
  public @NonNull IntoResponse<?> handleModuleConfigRequest(
    @NonNull @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_config_get"}) RestUser user,
    @NonNull @RequestPathParam("name") String name
  ) {
    return this.handleModuleContext(name, module -> {
      if (module.module() instanceof DriverModule driverModule) {
        var sensitiveData = driverModule.moduleConfig().storesSensitiveData();
        if (sensitiveData && !user.hasScope("cloudnet_rest:module_config_get_sensitive")) {
          return ProblemDetail.builder()
            .status(HttpResponseCode.FORBIDDEN)
            .type("missing-permission-module-sensitive-data")
            .title("Missing Permission Module Sensitive Data")
            .detail(String.format("The requested module %s stores sensitive data. Missing scope to read config", name));
        }

        var config = driverModule.readConfig(DocumentFactory.json());
        return JsonResponse.builder().body(config);
      } else {
        return ProblemDetail.builder()
          .status(HttpResponseCode.BAD_REQUEST)
          .type("module-misses-config-read-support")
          .title("Module Misses Config Read Support")
          .detail(String.format("The requested module %s does not support reading its configuration.", name));
      }
    });
  }

  @RequestHandler(path = "/api/v3/module/{name}/config", method = HttpMethod.PUT)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_config_update"})
  public @NonNull IntoResponse<?> handleModuleConfigRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestBody InputStream body
  ) {
    return this.handleModuleContext(name, module -> {
      if (module.module() instanceof DriverModule driverModule) {
        driverModule.writeConfig(DocumentFactory.json().parse(body));
        return HttpResponseCode.NO_CONTENT;
      } else {
        return ProblemDetail.builder()
          .status(HttpResponseCode.BAD_REQUEST)
          .type("module-misses-config-write-support")
          .title("Module Misses Config Write Support")
          .detail(String.format("The requested module %s does not support writing its configuration.", name));
      }
    });
  }

  private @NonNull Map<String, Object> constructModuleInformation(@NonNull ModuleWrapper module) {
    return Map.of("lifecycle", module.moduleLifeCycle(), "configuration", module.moduleConfiguration());
  }

  private @NonNull IntoResponse<?> handleModuleContext(
    @NonNull String name,
    @NonNull Function<ModuleWrapper, IntoResponse<?>> mapper
  ) {
    var module = this.moduleProvider.module(name);
    if (module == null) {
      return ProblemDetail.builder()
        .type("module-not-found")
        .title("Module Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested module %s was not found.", name));
    }

    return mapper.apply(module);
  }
}
