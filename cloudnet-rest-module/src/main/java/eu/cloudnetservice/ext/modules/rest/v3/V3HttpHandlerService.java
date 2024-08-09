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

import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceConfigurationDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceDeploymentDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceRemoteInclusionDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceTaskDto;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceTemplateDto;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketChannel;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketFrameType;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketListener;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class V3HttpHandlerService {

  private final CloudServiceFactory serviceFactory;
  private final CloudServiceManager serviceManager;
  private final ServiceTaskProvider serviceTaskProvider;

  @Inject
  public V3HttpHandlerService(
    @NonNull CloudServiceFactory serviceFactory,
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceTaskProvider serviceTaskProvider
  ) {
    this.serviceFactory = serviceFactory;
    this.serviceManager = serviceManager;
    this.serviceTaskProvider = serviceTaskProvider;
  }

  @RequestHandler(path = "/api/v3/service")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_list"})
  public @NonNull IntoResponse<?> handleServiceListRequest() {
    return JsonResponse.builder().body(Map.of("services", this.serviceManager.services()));
  }

  @RequestHandler(path = "/api/v3/service/{id}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_get"})
  public @NonNull IntoResponse<?> handleServiceGetRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleServiceContext(id, service -> JsonResponse.builder().body(service));
  }

  @RequestHandler(path = "/api/v3/service/{id}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_delete"})
  public @NonNull IntoResponse<?> handleServiceDeleteRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleEmptyServiceProviderContext(id, SpecificCloudServiceProvider::delete);
  }

  @RequestHandler(path = "/api/v3/service/{id}/deleteFiles", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_delete_files"})
  public @NonNull IntoResponse<?> handleServiceDeleteFilesRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleEmptyServiceProviderContext(id, SpecificCloudServiceProvider::deleteFiles);
  }

  @RequestHandler(path = "/api/v3/service/{id}/lifecycle", method = HttpMethod.PATCH)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_lifecycle"})
  public @NonNull IntoResponse<?> handleServiceLifecycleRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("target") String lifecycle
  ) {
    return this.handleServiceProviderContext(id, service -> {
      switch (StringUtil.toLower(lifecycle)) {
        case "start" -> service.start();
        case "stop" -> service.stop();
        case "restart" -> service.restart();
        default -> {
          return ProblemDetail.builder()
            .type("invalid-service-lifecycle")
            .title("Invalid Service Lifecycle")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail(String.format(
              "The requested service lifecycle %s does not exist. Supported values are: start, stop, restart",
              lifecycle
            ));
        }
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  @RequestHandler(path = "/api/v3/service/{id}/command", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_send_commands"})
  public @NonNull IntoResponse<?> handleServiceCommandRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @RequestTypedBody Map<String, String> command
  ) {
    return this.handleServiceProviderContext(id, service -> {
      var commandLine = command.get("command");
      if (commandLine == null) {
        return ProblemDetail.builder()
          .title("Missing Command Line")
          .type("missing-command-line")
          .status(HttpResponseCode.BAD_REQUEST)
          .detail("The request body has no 'command' field.");
      }

      service.runCommand(commandLine);
      return HttpResponseCode.NO_CONTENT;
    });
  }

  @RequestHandler(path = "/api/v3/service/{id}/include", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_include"})
  public @NonNull IntoResponse<?> handleServiceIncludeRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("type") String type
  ) {
    return this.handleServiceProviderContext(id, service -> {
      switch (StringUtil.toLower(type)) {
        case "templates" -> service.includeWaitingServiceTemplates();
        case "inclusions" -> service.includeWaitingServiceInclusions();
        default -> {
          return ProblemDetail.builder()
            .type("invalid-include-type")
            .title("Invalid Include Type")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail(String.format(
              "The requested inclusion type %s does not exist. Supported values are: templates, inclusions",
              type
            ));
        }
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  @RequestHandler(path = "/api/v3/service/{id}/deployResources", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_deploy_resources"})
  public @NonNull IntoResponse<?> handleServiceDeployRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam(value = "remove", def = "true") String remove
  ) {
    return this.handleEmptyServiceProviderContext(id, service -> service.deployResources(Boolean.parseBoolean(remove)));
  }

  @RequestHandler(path = "/api/v3/service/{id}/logLines")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_log_lines"})
  public @NonNull IntoResponse<?> handleServiceLogRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleServiceProviderContext(
      id,
      service -> JsonResponse.builder().body(Map.of("lines", service.cachedLogMessages())));
  }

  @RequestHandler(path = "/api/v3/service/{id}/liveLog")
  public @NonNull IntoResponse<?> handleServiceLiveLogRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull HttpContext context,
    @Authentication(
      providers = {"ticket", "jwt"},
      scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_live_log"}) @NonNull RestUser restUser
  ) {
    return this.handleServiceContext(id, service -> {
      var localService = this.serviceManager.localCloudService(service.serviceId().uniqueId());
      if (localService == null) {
        return ProblemDetail.builder()
          .type("service-not-on-local-node")
          .title("Service Not On Local Node")
          .status(HttpResponseCode.NOT_FOUND)
          .detail(String.format("The requested service %s is not running on the node the request was sent to.", id));
      }

      context.upgrade().thenAccept(channel -> {
        ServiceConsoleLineHandler handler = (console, line, stderr) -> channel.sendWebSocketFrame(
          WebSocketFrameType.TEXT,
          line);
        localService.serviceConsoleLogCache().addHandler(handler);

        channel.addListener(new ConsoleHandlerWebSocketListener(
          restUser,
          localService,
          localService.serviceConsoleLogCache(),
          handler));
      });

      return HttpResponseCode.SWITCHING_PROTOCOLS;
    });
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/service/create/serviceConfig", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_create_service_config"})
  public @NonNull IntoResponse<?> handleServiceCreateRequest(
    @Optional @FirstRequestQueryParam(value = "start", def = "false") String start,
    @Nullable @Valid @RequestTypedBody ServiceConfigurationDto configuration
  ) {
    if (configuration == null) {
      return ProblemDetail.builder()
        .type("missing-service-configuration")
        .title("Missing Service Configuration")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain a service configuration.");
    }

    return this.handleServiceCreate(configuration.toEntity(), start);
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/service/create/task", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_create_task"})
  public @NonNull IntoResponse<?> handleServiceCreateRequest(
    @Optional @FirstRequestQueryParam(value = "start", def = "false") String start,
    @Nullable @Valid @RequestTypedBody ServiceTaskDto task
  ) {
    if (task == null) {
      return ProblemDetail.builder()
        .type("missing-service-task")
        .title("Missing Service Task")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain a service task.");
    }

    var configuration = ServiceConfiguration.builder(task.toEntity()).build();
    return this.handleServiceCreate(configuration, start);
  }

  @RequestHandler(path = "/api/v3/service/create/taskName", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_create_task_name"})
  public @NonNull IntoResponse<?> handleServiceCreateRequest(
    @Optional @FirstRequestQueryParam(value = "start", def = "false") String start,
    @NonNull @RequestTypedBody Map<String, String> body
  ) {
    var taskName = body.get("taskName");
    if (taskName == null) {
      return ProblemDetail.builder()
        .type("missing-service-task-name")
        .title("Missing Service Task Name")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain a 'taskName' field.");
    }

    var serviceTask = this.serviceTaskProvider.serviceTask(taskName);
    if (serviceTask == null) {
      return ProblemDetail.builder()
        .type("task-not-found")
        .title("Task Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested task %s to start the service with was not found.", taskName));
    }

    var configuration = ServiceConfiguration.builder(serviceTask).build();
    return this.handleServiceCreate(configuration, start);
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/service/{id}/add/template", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_add_template"})
  public @NonNull IntoResponse<?> handleServiceAddTemplateRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @Optional @FirstRequestQueryParam(value = "flush", def = "false") String flush,
    @Nullable @RequestTypedBody @Valid ServiceTemplateDto templateDto
  ) {
    if (templateDto == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.BAD_REQUEST)
        .type("service-add-missing-template")
        .title("Service Add Missing Template")
        .detail("The request body does not contain a service template to add to the service.");
    }

    return this.handleServiceProviderContext(id, provider -> {
      provider.addServiceTemplate(templateDto.toEntity());
      if (Boolean.parseBoolean(flush)) {
        provider.includeWaitingServiceTemplates();
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/service/{id}/add/deployment", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_add_deployment"})
  public @NonNull IntoResponse<?> handleServiceAddDeploymentRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @Optional @FirstRequestQueryParam(value = "flush", def = "false") String flush,
    @NonNull @Optional @FirstRequestQueryParam(value = "removeDeployment", def = "false") String remove,
    @Nullable @RequestTypedBody @Valid ServiceDeploymentDto deploymentDto
  ) {
    if (deploymentDto == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.BAD_REQUEST)
        .type("service-add-missing-template")
        .title("Service Add Missing Template")
        .detail("The request body does not contain a service template to add to the service.");
    }

    return this.handleServiceProviderContext(id, provider -> {
      provider.addServiceDeployment(deploymentDto.toEntity());
      if (Boolean.parseBoolean(flush)) {
        provider.deployResources(Boolean.parseBoolean(remove));
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/service/{id}/add/inclusion", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_add_inclusion"})
  public @NonNull IntoResponse<?> handleServiceAddInclusionRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @Optional @FirstRequestQueryParam(value = "flush", def = "false") String flush,
    @Nullable @RequestTypedBody @Valid ServiceRemoteInclusionDto inclusionDto
  ) {
    if (inclusionDto == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.BAD_REQUEST)
        .type("service-add-missing-inclusion")
        .title("Service Add Missing Inclusion")
        .detail("The request body does not contain a service inclusion to add to the service.");
    }

    return this.handleServiceProviderContext(id, provider -> {
      provider.addServiceRemoteInclusion(inclusionDto.toEntity());
      if (Boolean.parseBoolean(flush)) {
        provider.includeWaitingServiceInclusions();
      }

      return HttpResponseCode.NO_CONTENT;
    });
  }

  private @NonNull IntoResponse<?> handleServiceCreate(
    @NonNull ServiceConfiguration configuration,
    @NonNull String start
  ) {
    var createResult = this.serviceFactory.createCloudService(configuration);
    if (createResult.state() == ServiceCreateResult.State.CREATED && Boolean.parseBoolean(start)) {
      createResult.serviceInfo().provider().start();
    }

    return JsonResponse.builder().responseCode(HttpResponseCode.CREATED).body(createResult);
  }

  private @NonNull IntoResponse<?> handleServiceProviderContext(
    @NonNull String id,
    @NonNull Function<SpecificCloudServiceProvider, IntoResponse<?>> mapper
  ) {
    // try to find a matching service
    SpecificCloudServiceProvider serviceProvider;
    try {
      // try to parse a unique id from that
      var serviceId = UUID.fromString(id);
      serviceProvider = this.serviceManager.serviceProvider(serviceId);
    } catch (Exception exception) {
      serviceProvider = this.serviceManager.serviceProviderByName(id);
    }

    if (!serviceProvider.valid()) {
      return ProblemDetail.builder()
        .type("service-not-found")
        .title("Service Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested service %s was not found.", id));
    }

    return mapper.apply(serviceProvider);
  }

  private @NonNull IntoResponse<?> handleEmptyServiceProviderContext(
    @NonNull String id,
    @NonNull Consumer<SpecificCloudServiceProvider> mapper
  ) {
    return this.handleServiceProviderContext(id, service -> {
      mapper.accept(service);
      return HttpResponseCode.NO_CONTENT;
    });
  }

  private @NonNull IntoResponse<?> handleServiceContext(
    @NonNull String id,
    @NonNull Function<ServiceInfoSnapshot, IntoResponse<?>> mapper
  ) {
    return this.handleServiceProviderContext(id, provider -> mapper.apply(provider.serviceInfo()));
  }

  record ConsoleHandlerWebSocketListener(
    @NonNull RestUser user,
    @NonNull CloudService service,
    @NonNull ServiceConsoleLogCache logCache,
    @NonNull ServiceConsoleLineHandler watchingHandler
  ) implements WebSocketListener {

    private static final Set<String> REQUIRED_SCOPES = Set.of(
      "cloudnet_rest:service_write",
      "cloudnet_rest:service_send_commands");

    @Override
    public void handle(
      @NonNull WebSocketChannel channel,
      @NonNull WebSocketFrameType type,
      byte[] bytes
    ) {
      if (type == WebSocketFrameType.TEXT && this.user.hasOneScopeOf(REQUIRED_SCOPES)) {
        var commandLine = new String(bytes, StandardCharsets.UTF_8);
        this.service.runCommand(commandLine);
      }
    }

    @Override
    public void handleClose(
      @NonNull WebSocketChannel channel,
      @NonNull AtomicInteger statusCode,
      @NonNull AtomicReference<String> reasonText
    ) {
      this.logCache.removeHandler(this.watchingHandler);
    }
  }
}
