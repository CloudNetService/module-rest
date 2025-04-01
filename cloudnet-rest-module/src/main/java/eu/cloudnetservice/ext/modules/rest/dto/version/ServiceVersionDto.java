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

package eu.cloudnetservice.ext.modules.rest.dto.version;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import eu.cloudnetservice.node.impl.version.ServiceVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.NonNull;

public final class ServiceVersionDto implements Dto<ServiceVersion> {

  @NotBlank
  private final String name;
  @NotNull
  private final String url;

  private final int minJavaVersion;
  private final int maxJavaVersion;

  private final boolean deprecated;
  private final boolean cacheFiles;

  @NotNull
  private final Document properties;
  @NotNull
  private final Map<String, String> additionalDownloads;

  public ServiceVersionDto(
    String name,
    String url,
    int minJavaVersion,
    int maxJavaVersion,
    boolean deprecated,
    boolean cacheFiles,
    Document properties,
    Map<String, String> additionalDownloads
  ) {
    this.name = name;
    this.url = url;
    this.minJavaVersion = minJavaVersion;
    this.maxJavaVersion = maxJavaVersion;
    this.deprecated = deprecated;
    this.cacheFiles = cacheFiles;
    this.properties = properties;
    this.additionalDownloads = additionalDownloads;
  }


  @Override
  public @NonNull ServiceVersion toEntity() {
    return new ServiceVersion(
      this.name,
      this.url,
      this.minJavaVersion,
      this.maxJavaVersion,
      this.deprecated,
      this.cacheFiles,
      this.properties,
      this.additionalDownloads);
  }
}
