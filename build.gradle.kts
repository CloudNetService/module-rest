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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import java.nio.charset.StandardCharsets

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.nexusPublish)
}

allprojects {
  version = "0.4.0-SNAPSHOT"
  group = "eu.cloudnetservice.ext"

  apply(plugin = "signing")
  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")
  apply(plugin = "com.diffplug.spotless")

  repositories {
    mavenCentral()
    maven("https://repository.derklaro.dev/releases/")
    maven("https://repository.derklaro.dev/snapshots/")
  }

  dependencies {
    "compileOnly"(rootProject.libs.annotations)
    "compileOnly"(rootProject.libs.lombok)
    "annotationProcessor"(rootProject.libs.lombok)

    "testImplementation"(rootProject.libs.mockito)
    "testImplementation"(rootProject.libs.testContainers)
    "testImplementation"(rootProject.libs.testContainersJunit)

    "testRuntimeOnly"(rootProject.libs.junitEngine)
    "testRuntimeOnly"(rootProject.libs.junitLauncher)
    "testImplementation"(rootProject.libs.junitApi)
    "testImplementation"(rootProject.libs.junitParams)
  }

  configurations.all {
    // unsure why but every project loves them, and they literally have an import for every letter I type - beware
    exclude("org.checkerframework", "checker-qual")
  }

  tasks.withType<Jar> {
    from(rootProject.file("license.txt"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
      events("started", "passed", "skipped", "failed")
    }

    // allow dynamic agent loading for mockito
    jvmArgs(
      "-XX:+EnableDynamicAgentLoading",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
    )

    // always pass down all given system properties
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
    systemProperty("io.netty5.noUnsafe", "true")

    // forces the re-run of tests everytime the task is executed
    outputs.upToDateWhen { false }
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()

    options.encoding = "UTF-8"
    options.isIncremental = true

    options.compilerArgs.add("-proc:none")
    options.compilerArgs.addAll(
      listOf(
        "-Xlint:all",         // enable all warnings
        "-Xlint:-preview",    // reduce warning size for the following warning types
        "-Xlint:-unchecked",
        "-Xlint:-classfile",
        "-Xlint:-processing",
        "-Xlint:-deprecation",
      )
    )
  }

  tasks.withType<Checkstyle> {
    maxErrors = 0
    maxWarnings = 0
    configFile = rootProject.file("checkstyle.xml")
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = rootProject.libs.versions.checkstyleTools.get()
  }

  extensions.configure<SpotlessExtension> {
    java {
      lineEndings = LineEnding.UNIX
      encoding = StandardCharsets.UTF_8
      licenseHeaderFile(rootProject.file("license_header.txt"))
    }
  }
}
