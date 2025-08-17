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

import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import lombok.NonNull;

@Singleton
public final class V3HttpHandlerTemplateStorage {

  private final TemplateStorageProvider storageProvider;

  @Inject
  public V3HttpHandlerTemplateStorage(@NonNull TemplateStorageProvider storageProvider) {
    this.storageProvider = storageProvider;
  }

  @RequestHandler(path = "/api/v3/templateStorage")
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:template_storage_read", "cloudnet_rest:template_storage_list"})
  public @NonNull IntoResponse<?> handleTemplateStorageListRequest() {
    return JsonResponse.builder().body(Map.of("storages", this.storageProvider.availableTemplateStorages()));
  }

  @RequestHandler(path = "/api/v3/templateStorage/{storage}/templates")
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:template_storage_read", "cloudnet_rest:template_storage_template_list"})
  public @NonNull IntoResponse<?> handleTemplateStorageListTemplatesRequest(
    @NonNull @RequestPathParam("storage") String storage
  ) {
    var templateStorage = this.storageProvider.templateStorage(storage);
    if (templateStorage == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.NOT_FOUND)
        .type("template-storage-not-found")
        .title("Template Storage Not Found")
        .detail(String.format("The requested template storage %s was not found.", storage));
    }

    return JsonResponse.builder().body(Map.of("templates", templateStorage.templates()));
  }
}
