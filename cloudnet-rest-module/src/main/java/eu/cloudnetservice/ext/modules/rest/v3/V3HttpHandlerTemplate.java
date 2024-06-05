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

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestBody;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.InputStreamResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class V3HttpHandlerTemplate {

  private static final Logger LOGGER = LogManager.logger(V3HttpHandlerTemplate.class);

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/download")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_read", "cloudnet_rest:template_download"})
  public @NonNull IntoResponse<?> handleTemplateDownloadRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      var stream = storage.zipTemplate(template);
      if (stream == null) {
        return ProblemDetail.builder()
          .type("template-not-found")
          .title("Template Not Found")
          .status(HttpResponseCode.NOT_FOUND)
          .detail(String.format("The requested template %s does not exist", template));
      }

      return this.applyDispositionHeader(MediaType.ZIP, template.toString().replace('/', '_') + ".zip").body(stream);
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/file/download")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_read", "cloudnet_rest:template_file_download"})
  public @NonNull IntoResponse<?> handleTemplateFileDownloadRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      var stream = storage.newInputStream(template, path);
      if (stream == null) {
        return ProblemDetail.builder()
          .type("template-file-not-found")
          .title("Template File Not Found")
          .status(HttpResponseCode.NOT_FOUND)
          .detail(String.format("The requested template %s does not contain the requested file %s", template, path));
      }

      return this.applyDispositionHeader(MediaType.OCTET_STREAM, this.guessFileName(path)).body(stream);
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/file/info")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_read", "cloudnet_rest:template_file_info"})
  public @NonNull IntoResponse<?> handleTemplateFileInfoRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      var fileInfo = storage.fileInfo(template, path);
      if (fileInfo == null) {
        return ProblemDetail.builder()
          .type("template-file-not-found")
          .title("Template File Not Found")
          .status(HttpResponseCode.NOT_FOUND)
          .detail(String.format("The requested template %s does not contain the requested file %s", template, path));
      }

      return JsonResponse.builder().body(fileInfo);
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/file/exists")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_read", "cloudnet_rest:template_file_exists"})
  public @NonNull IntoResponse<?> handleTemplateFileExistsRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    return this.handleTemplateContext(
      storageName,
      prefix,
      name,
      (template, storage) -> JsonResponse.builder().body(Map.of("exists", storage.hasFile(template, path))));
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/directory/list")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_read", "cloudnet_rest:template_directory_list"})
  public @NonNull IntoResponse<?> handleTemplateDirectoryListRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @Optional @FirstRequestQueryParam(value = "directory", def = "") String directory,
    @NonNull @Optional @FirstRequestQueryParam(value = "deep", def = "false") String deep
  ) {
    return this.handleTemplateContext(
      storageName,
      prefix,
      name,
      (template, storage) -> JsonResponse.builder()
        .body(Map.of("files", storage.listFiles(template, directory, Boolean.parseBoolean(deep)))));
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/create", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_create"})
  public @NonNull IntoResponse<?> handleTemplateCreateRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      if (storage.create(template)) {
        return HttpResponseCode.NO_CONTENT;
      }

      return ProblemDetail.builder()
        .type("template-already-exists")
        .title("Template Already Exists")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail(String.format(
          "The requested template %s already exists in the provided template storage %s",
          template,
          storageName));
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/exists")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_read", "cloudnet_rest:template_exists"})
  public @NonNull IntoResponse<?> handleTemplateExistsRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name
  ) {
    return this.handleTemplateContext(
      storageName,
      prefix,
      name,
      (template, storage) -> JsonResponse.builder().body(Map.of("exists", storage.contains(template))));
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/file", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_delete_file"})
  public @NonNull IntoResponse<?> handleTemplateDeleteFileRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      if (storage.deleteFile(template, path)) {
        return HttpResponseCode.NO_CONTENT;
      }

      return ProblemDetail.builder()
        .type("template-file-not-found")
        .title("Template File Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested template %s does not contain the requested file %s", template, path));
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_delete"})
  public @NonNull IntoResponse<?> handleTemplateDeleteRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      if (storage.delete(template)) {
        return HttpResponseCode.NO_CONTENT;
      }

      return ProblemDetail.builder()
        .type("template-not-found")
        .title("Template Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested template %s does not exist", template));
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/directory/create", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_directory_create"})
  public @NonNull IntoResponse<?> handleTemplateDirectoryCreateRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      if (storage.createDirectory(template, path)) {
        return HttpResponseCode.NO_CONTENT;
      }

      return ProblemDetail.builder()
        .status(HttpResponseCode.BAD_REQUEST)
        .type("template-create-directory-already-exists")
        .title("Template Create Directory Already Exists")
        .detail(String.format("The requested directory %s already exists in template %s", path, template));
    });
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/file/create", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_file_create"})
  public @NonNull IntoResponse<?> handleTemplateFileCreateRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path,
    @NonNull @RequestBody InputStream body
  ) {
    return this.handleFileRequest(storageName, prefix, name, path, body, false);
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/file/append", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_file_append"})
  public @NonNull IntoResponse<?> handleTemplateFileAppendRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("path") String path,
    @NonNull @RequestBody InputStream body
  ) {
    return this.handleFileRequest(storageName, prefix, name, path, body, true);
  }

  @RequestHandler(path = "/api/v3/template/{storage}/{prefix}/{name}/deploy", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:template_write", "cloudnet_rest:template_deploy"})
  public @NonNull IntoResponse<?> handleTemplateDeployRequest(
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestBody InputStream body
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      storage.deploy(template, body);
      return HttpResponseCode.NO_CONTENT;
    });
  }

  private @NonNull IntoResponse<?> handleFileRequest(
    @NonNull String storageName,
    @NonNull String prefix,
    @NonNull String name,
    @NonNull String path,
    @NonNull InputStream body,
    boolean append
  ) {
    return this.handleTemplateContext(storageName, prefix, name, (template, storage) -> {
      var stream = append ? storage.appendOutputStream(template, path) : storage.newOutputStream(template, path);
      if (stream == null) {
        // yek this can't happen
        return ProblemDetail.builder()
          .type("template-open-stream-failed")
          .title("Template Open Stream Failed")
          .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
          .detail("The handling of the requested template action failed due to an internal i/o error.");
      }

      body.transferTo(stream);
      stream.close();
      return HttpResponseCode.NO_CONTENT;
    });
  }

  private @NonNull IntoResponse<?> handleTemplateContext(
    @NonNull String storage,
    @NonNull String prefix,
    @NonNull String name,
    @NonNull ThrowableBiFunction<ServiceTemplate, TemplateStorage, IOException> mapper
  ) {
    var template = ServiceTemplate.builder().prefix(prefix).name(name).storage(storage).build();
    var templateStorage = template.findStorage();
    if (templateStorage == null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.NOT_FOUND)
        .type("template-storage-not-found")
        .title("Template Storage Not Found")
        .detail(String.format("The requested template storage %s was not found.", storage));
    }

    try {
      return mapper.apply(template, templateStorage);
    } catch (IOException exception) {
      LOGGER.fine("Exception handling template request", exception);
      return ProblemDetail.builder()
        .type("template-handling-failed")
        .title("Template Handling Failed")
        .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
        .detail("The handling of the requested template action failed due to an internal i/o error.");
    }
  }

  @FunctionalInterface
  private interface ThrowableBiFunction<T, U, E extends Throwable> {

    @NonNull
    IntoResponse<?> apply(@NonNull T t, @NonNull U u) throws E;
  }

  private @NonNull InputStreamResponse.Builder applyDispositionHeader(
    @NonNull MediaType type,
    @Nullable String fileName
  ) {
    return InputStreamResponse.builder().header(
        HttpHeaders.CONTENT_DISPOSITION,
        String.format("attachment%s", fileName == null ? "" : "; filename=" + fileName))
      .contentType(type.toString());
  }

  private @Nullable String guessFileName(@NonNull String path) {
    var index = path.lastIndexOf('/');
    if (index == -1 || index + 1 == path.length()) {
      return null;
    } else {
      return path.substring(index);
    }
  }

}
