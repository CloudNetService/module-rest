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

package eu.cloudnetservice.ext.modules.rest.v3;

import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.ext.modules.rest.dto.GroupConfigurationDto;
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
public final class V3HttpHandlerGroup {

  private final GroupConfigurationProvider groupProvider;

  @Inject
  public V3HttpHandlerGroup(@NonNull GroupConfigurationProvider groupProvider) {
    this.groupProvider = groupProvider;
  }

  @RequestHandler(path = "/api/v3/group")
  @Authentication(providers = "jwt", scopes = {"cloudnet_cloudnet_rest:group_read", "cloudnet_rest:group_list"})
  public @NonNull IntoResponse<?> handleGroupListRequest() {
    return JsonResponse.builder().body(Map.of("groups", this.groupProvider.groupConfigurations()));
  }

  @RequestHandler(path = "/api/v3/group/{name}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:group_read", "cloudnet_rest:group_get"})
  public @NonNull IntoResponse<?> handleGroupGetRequest(@NonNull @RequestPathParam("name") String name) {
    var group = this.groupProvider.groupConfiguration(name);
    if (group == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.NOT_FOUND)
        .type("group-configuration-not-found")
        .title("Group Configuration Not Found")
        .detail(String.format("The group configuration %s was not found.", name));
    }

    return JsonResponse.builder().body(group);
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/group", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:group_write", "cloudnet_rest:group_create"})
  public @NonNull IntoResponse<?> handleGroupCreateRequest(@Valid @RequestTypedBody GroupConfigurationDto group) {
    if (group == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.NOT_FOUND)
        .type("missing-group-configuration")
        .title("Missing Group Configuration")
        .detail("The request body does not contain a group configuration.");
    }

    this.groupProvider.addGroupConfiguration(group.toEntity());
    return HttpResponseCode.CREATED;
  }

  @RequestHandler(path = "/api/v3/group/{name}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:group_write", "cloudnet_rest:group_delete"})
  public @NonNull IntoResponse<?> handleGroupDeleteRequest(@NonNull @RequestPathParam("name") String name) {
    var group = this.groupProvider.groupConfiguration(name);
    if (group == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.NOT_FOUND)
        .type("group-configuration-not-found")
        .title("Group Configuration Not Found")
        .detail(String.format("The group configuration %s was not found.", name));
    }

    this.groupProvider.removeGroupConfiguration(group);
    return HttpResponseCode.NO_CONTENT;
  }

}
