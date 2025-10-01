import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

plugins {
  alias(libs.plugins.shadow)
  alias(libs.plugins.juppiter)
}

repositories {
  maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
  api(projects.webApi)
  moduleLibrary(libs.guava)
  moduleLibrary(libs.unirest)
  moduleLibrary(libs.caffeine)

  implementation(projects.webJwtAuth)
  moduleLibrary(libs.jjwtApi)
  moduleLibrary(libs.jjwtImpl)
  moduleLibrary(libs.jjwtGson)

  implementation(projects.webTicketAuth)

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

  compileOnly(libs.logbackCore)
  compileOnly(libs.logbackClassic)

  compileOnly("eu.cloudnetservice.cloudnet:node-impl:4.0.0-RC15-SNAPSHOT")
  compileOnly("eu.cloudnetservice.cloudnet:bridge-impl:4.0.0-RC15-SNAPSHOT")
}

tasks.withType<Test> {
  jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
  sourceCompatibility = JavaVersion.VERSION_25.toString()
  targetCompatibility = JavaVersion.VERSION_25.toString()
  options.compilerArgs.add("--enable-preview")
}

tasks.withType<Javadoc> {
  val options = options as? StandardJavadocDocletOptions ?: return@withType
  options.addStringOption("-release", "25")
  options.addBooleanOption("-enable-preview", true)
}

extensions.configure<JavaPluginExtension> {
  toolchain {
    vendor = JvmVendorSpec.AZUL
    languageVersion = JavaLanguageVersion.of(25)
  }
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
  version = project.version.toString()
  author = "CloudNetService"
  description = "Module which provides a REST API for managing resources within the CloudNet system"
  runtimeModule = true
}
