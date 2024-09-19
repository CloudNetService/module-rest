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

package eu.cloudnetservice.ext.modules.rest.config;

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.ext.rest.api.connection.HttpConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.connection.parse.ForwardedSyntaxConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.connection.parse.HostHeaderConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.api.connection.parse.XForwardSyntaxConnectionInfoResolver;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record ConnectionInfoResolverConfiguration(
  @NonNull String type,
  @NonNull Document properties
) implements DefaultedDocPropertyHolder {

  private static final DocProperty<String> FORWARDED_HEADER = DocProperty
    .property("forwardedHeader", String.class);
  private static final DocProperty<String> X_FORWARDED_HOST_HEADER = DocProperty
    .property("forwardedHostHeader", String.class)
    .withDefault(HttpHeaders.X_FORWARDED_HOST);
  private static final DocProperty<String> X_FORWARDED_PORT_HEADER = DocProperty
    .property("forwardedPortHeader", String.class)
    .withDefault(HttpHeaders.X_FORWARDED_PORT);
  private static final DocProperty<String> X_FORWARDED_PROTO_HEADER = DocProperty
    .property("forwardedProtoHeader", String.class)
    .withDefault(HttpHeaders.X_FORWARDED_PROTO);

  public @Nullable HttpConnectionInfoResolver toResolver() {
    return switch (StringUtil.toLower(this.type)) {
      case "host" -> HostHeaderConnectionInfoResolver.INSTANCE;
      case "forwarded" -> {
        var forwarded = this.readPropertyOrDefault(FORWARDED_HEADER, HttpHeaders.FORWARDED);
        yield new ForwardedSyntaxConnectionInfoResolver(forwarded);
      }
      case "x-forwarded" -> {
        var forwardedFor = this.readPropertyOrDefault(FORWARDED_HEADER, HttpHeaders.X_FORWARDED_FOR);
        var forwardedHost = this.readProperty(X_FORWARDED_HOST_HEADER);
        var forwardedPort = this.readProperty(X_FORWARDED_PORT_HEADER);
        var forwardedProto = this.readProperty(X_FORWARDED_PROTO_HEADER);
        yield new XForwardSyntaxConnectionInfoResolver(forwardedFor, forwardedHost, forwardedPort, forwardedProto);
      }
      default -> null;
    };
  }

  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }
}
