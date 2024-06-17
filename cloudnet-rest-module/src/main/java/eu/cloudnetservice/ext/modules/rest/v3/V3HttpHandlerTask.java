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

import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.ext.modules.rest.dto.service.ServiceTaskDto;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.NonNull;

@Singleton
public final class V3HttpHandlerTask {

  private final ServiceTaskProvider taskProvider;

  @Inject
  public V3HttpHandlerTask(@NonNull ServiceTaskProvider taskProvider) {
    this.taskProvider = taskProvider;
  }

  @RequestHandler(path = "/api/v3/task")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:task_read", "cloudnet_rest:task_list"})
  public @NonNull IntoResponse<?> handleTaskListRequest() {
    return JsonResponse.builder().body(Map.of("tasks", this.taskProvider.serviceTasks()));
  }

  @RequestHandler(path = "/api/v3/task/{name}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:task_read", "cloudnet_rest:task_get"})
  public @NonNull IntoResponse<?> handleTaskGetRequest(@NonNull @RequestPathParam("name") String name) {
    var task = this.taskProvider.serviceTask(name);
    if (task == null) {
      return this.taskNotFound(name);
    }

    return JsonResponse.builder().body(task);
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/task", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:task_write", "cloudnet_rest:task_create"})
  public @NonNull IntoResponse<?> handleTaskCreateRequest(@Valid @RequestTypedBody ServiceTaskDto serviceTask) {
    if (serviceTask == null) {
      return ProblemDetail.builder()
        .type("missing-service-task")
        .title("Missing Service Task")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain a service task.");
    }

    this.taskProvider.addServiceTask(serviceTask.toEntity());
    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/task/{name}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:task_write", "cloudnet_rest:task_delete"})
  public @NonNull IntoResponse<?> handleTaskDeleteRequest(@NonNull @RequestPathParam("name") String name) {
    var task = this.taskProvider.serviceTask(name);
    if (task == null) {
      return this.taskNotFound(name);
    }

    this.taskProvider.removeServiceTask(task);
    return HttpResponseCode.NO_CONTENT;
  }

  private @NonNull ProblemDetail taskNotFound(@NonNull String name) {
    return ProblemDetail.builder()
      .type("service-task-not-found")
      .title("Service Task Not Found")
      .status(HttpResponseCode.NOT_FOUND)
      .detail(String.format("The requested service task %s was not found.", name))
      .build();
  }
}
