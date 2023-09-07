/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.modules.rest.v2;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import eu.cloudnetservice.node.version.ServiceVersionType;
import eu.cloudnetservice.node.version.information.FileSystemVersionInstaller;
import eu.cloudnetservice.node.version.information.TemplateVersionInstaller;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class V2HttpHandlerServiceVersion {

  private final CloudServiceManager serviceManager;
  private final ServiceVersionProvider versionProvider;

  @Inject
  public V2HttpHandlerServiceVersion(
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceVersionProvider versionProvider
  ) {
    this.serviceManager = serviceManager;
    this.versionProvider = versionProvider;
  }

  @RequestHandler(path = "/api/v2/serviceversion")
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_version_read", "cloudnet_rest:service_version_list"})
  public @NonNull IntoResponse<?> handleServiceVersionListRequest() {
    return JsonResponse.builder().body(Map.of("versions", this.versionProvider.serviceVersionTypes()));
  }

  @RequestHandler(path = "/api/v2/serviceversion/{version}")
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_version_read", "cloudnet_rest:service_version_get"})
  public @NonNull IntoResponse<?> handleServiceVersionRequest(@NonNull @RequestPathParam("version") String version) {
    var versionType = this.versionProvider.getServiceVersionType(version);
    if (versionType == null) {
      return ProblemDetail.builder()
        .type("service-version-not-found")
        .title("Service Version Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested service version %s was not found.", version));
    }

    return JsonResponse.builder().body(Map.of("version", versionType));
  }

  @RequestHandler(path = "/api/v2/serviceversion/load", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_version_write", "cloudnet_rest:service_version_load"})
  public @NonNull IntoResponse<?> handleServiceVersionLoadRequest(
    @Nullable @Optional @FirstRequestQueryParam("url") String url
  ) {
    if (url == null) {
      this.versionProvider.loadDefaultVersionTypes();
    } else {
      try {
        if (!this.versionProvider.loadServiceVersionTypes(url)) {
          return ProblemDetail.builder()
            .type("service-version-invalid")
            .title("Service Version Invalid")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail("The service version document requested from the given url is invalid.");
        }
      } catch (IOException exception) {
        return ProblemDetail.builder()
          .type("service-version-load-failed")
          .title("Service Version Load Failed")
          .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
          .detail("The service version load failed due to an internal I/O error.");
      }
    }

    return JsonResponse.builder().noContent();
  }

  // TODO docs: new route
  @RequestHandler(path = "/api/v2/serviceversion/install", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:service_version_write", "cloudnet_rest:service_version_install"})
  public @NonNull IntoResponse<?> handleServiceVersionInstallRequest(
    @NonNull @RequestTypedBody Document body,
    @NonNull @Optional @FirstRequestQueryParam(value = "cache", def = "true") String cache,
    @NonNull @Optional @FirstRequestQueryParam(value = "force", def = "false") String force
  ) {
    var versionType = this.extractServiceVersionType(body);
    if (versionType == null) {
      return ProblemDetail.builder()
        .type("service-version-not-found")
        .title("Service Version Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail("The requested service version was not found.");
    }

    var exactVersion = this.extractServiceVersion(versionType, body);
    if (exactVersion == null) {
      return ProblemDetail.builder()
        .type("service-version-not-found")
        .title("Service Version Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested sub-version of version type %s was not found.", versionType.name()));
    }

    var template = body.readObject("template", ServiceTemplate.class);
    var staticService = body.getString("staticService");
    if (template == null && staticService == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.BAD_REQUEST)
        .type("service-version-install-missing-target")
        .title("Service Version Install Missing Target")
        .detail("The request body has no target."
          + " Either set a template in the 'template' field or a static service in the 'staticService' field.");
    }

    var enableCaches = Boolean.parseBoolean(cache);
    var forceInstall = Boolean.parseBoolean(force);
    // proceed as template installer
    if (template != null) {
      var templateStorage = template.findStorage();
      if (templateStorage == null) {
        return ProblemDetail.builder()
          .status(HttpResponseCode.NOT_FOUND)
          .type("service-version-install-template-storage-not-found")
          .title("Service Version Install Template Storage Not Found")
          .detail(String.format("The requested template storage %s was not found.", template.storageName()));

      }

      var templateInstaller = TemplateVersionInstaller.builder()
        .toTemplate(template)
        .cacheFiles(enableCaches)
        .serviceVersion(exactVersion)
        .serviceVersionType(versionType)
        .build();
      this.versionProvider.installServiceVersion(templateInstaller, forceInstall);
    } else {
      // proceed as static service installer
      var staticServiceDirectory = this.serviceManager.persistentServicesDirectory().resolve(staticService);
      FileUtil.ensureChild(this.serviceManager.persistentServicesDirectory(), staticServiceDirectory);

      var fileInstaller = FileSystemVersionInstaller.builder()
        .cacheFiles(enableCaches)
        .serviceVersion(exactVersion)
        .serviceVersionType(versionType)
        .workingDirectory(staticServiceDirectory)
        .build();
      this.versionProvider.installServiceVersion(fileInstaller, forceInstall);
    }

    return JsonResponse.builder().noContent();
  }

  private @Nullable ServiceVersionType extractServiceVersionType(@NonNull Document body) {
    var versionName = body.getString("serviceVersionType");
    if (versionName != null) {
      return this.versionProvider.getServiceVersionType(versionName);
    }

    // try to read the object from the body
    return body.readObject("serviceVersionType", ServiceVersionType.class);
  }

  private @Nullable ServiceVersion extractServiceVersion(@NonNull ServiceVersionType type, @NonNull Document body) {
    var versionName = body.getString("serviceVersion");
    if (versionName != null) {
      return type.version(versionName);
    }

    return body.readObject("serviceVersion", ServiceVersion.class);
  }
}
