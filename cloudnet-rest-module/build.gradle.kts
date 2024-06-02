import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

plugins {
  alias(libs.plugins.shadow)
  alias(libs.plugins.juppiter)
}

dependencies {
  api(projects.webApi)
  moduleLibrary(libs.guava)

  implementation(projects.webJwtAuth)
  moduleLibrary(libs.jjwtApi)
  moduleLibrary(libs.jjwtImpl)
  moduleLibrary(libs.jjwtGson)

  implementation(projects.webImplNetty)
  moduleLibrary(libs.nettyHandler)
  moduleLibrary(libs.nettyCodecHttp)
  moduleLibrary(libs.nettyContribHaProxy)

  // transports
  moduleLibrary(libs.nettyNativeEpoll)
  moduleLibrary(libs.nettyNativeKqueue)

  implementation(projects.webParameterValidator)
  moduleLibrary(libs.expressly)
  moduleLibrary(libs.hibernateValidator)
  moduleLibrary(libs.jakartaValidationApi)

  compileOnly("eu.cloudnetservice.cloudnet:node:4.0.0-RC10")
  compileOnly("eu.cloudnetservice.cloudnet:bridge:4.0.0-RC10")
}

tasks.withType<Jar> {
  archiveFileName.set("cloudnet-rest.jar")
}

tasks.withType<ShadowJar> {
  dependencies {
    include {
      it.moduleGroup.startsWith("eu.cloudnetservice")
    }
  }
}

moduleJson {
  main = "eu.cloudnetservice.ext.modules.rest.CloudNetRestModule"
  name = "CloudNet-Rest"
  version = "1.0"
  author = "CloudNetService"
  description = "The REST module offers a CRUD REST API for cloudnet"
  runtimeModule = true
}
