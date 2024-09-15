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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import eu.cloudnetservice.common.concurrent.TaskUtil;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.ext.modules.rest.dto.JsonConfigurationDto;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketChannel;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketFrameType;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketListener;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.DriverCommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.JsonConfiguration;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Singleton
public final class V3HttpHandlerNode {

  private final Logger logger;
  private final Configuration configuration;
  private final NetworkClient networkClient;
  private final ModuleProvider moduleProvider;
  private final CloudNetVersion cloudNetVersion;
  private final CommandProvider commandProvider;
  private final NodeServerProvider nodeServerProvider;
  private final CloudServiceManager cloudServiceManager;
  private final ServiceTaskProvider serviceTaskProvider;
  private final GroupConfigurationProvider groupConfigurationProvider;

  @Inject
  public V3HttpHandlerNode(
    @NonNull @Named("root") Logger logger,
    @NonNull Configuration configuration,
    @NonNull NetworkClient networkClient,
    @NonNull ModuleProvider moduleProvider,
    @NonNull CloudNetVersion cloudNetVersion,
    @NonNull CommandProvider commandProvider,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceManager cloudServiceManager,
    @NonNull ServiceTaskProvider serviceTaskProvider,
    @NonNull GroupConfigurationProvider groupConfigurationProvider
  ) {
    this.logger = logger;
    this.configuration = configuration;
    this.networkClient = networkClient;
    this.moduleProvider = moduleProvider;
    this.cloudNetVersion = cloudNetVersion;
    this.commandProvider = commandProvider;
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceManager = cloudServiceManager;
    this.serviceTaskProvider = serviceTaskProvider;
    this.groupConfigurationProvider = groupConfigurationProvider;
  }

  @RequestHandler(path = "/api/v3/node/ping")
  @Authentication(providers = {"basic", "jwt"}, scopes = {"cloudnet_rest:node_read", "cloudnet_rest:node_ping"})
  public @NonNull IntoResponse<?> handleNodePingRequest() {
    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/node")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:node_read", "cloudnet_rest:node_info"})
  public @NonNull IntoResponse<?> handleNodeInfoRequest() {
    var node = this.nodeServerProvider.localNode();
    var information = Map.of(
      "version", this.cloudNetVersion,
      "nodeInfoSnapshot", node.nodeInfoSnapshot(),
      "lastNodeInfoSnapshot", node.lastNodeInfoSnapshot(),
      "serviceCount", this.cloudServiceManager.serviceCount(),
      "clientConnections", this.networkClient.channels().stream().map(NetworkChannel::clientAddress).toList());
    return JsonResponse.builder().body(information);
  }

  @RequestHandler(path = "/api/v3/node/config")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:node_read", "cloudnet_rest:node_config_get"})
  public @NonNull IntoResponse<?> handleNodeConfigRequest() {
    return JsonResponse.builder().body(this.configuration);
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/node/config", method = HttpMethod.PUT)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:node_write", "cloudnet_rest:node_config_update"})
  public @NonNull IntoResponse<?> handleNodeConfigRequest(
    @Nullable @RequestTypedBody @Valid JsonConfigurationDto configurationDto
  ) {
    if (configurationDto == null) {
      return ProblemDetail.builder()
        .type("missing-node-configuration")
        .title("Missing Node Configuration")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain a node configuration.");
    }

    var config = configurationDto.toEntity();
    config.save();
    this.configuration.reloadFrom(config);

    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/node/reload", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:node_write", "cloudnet_rest:node_reload"})
  public @NonNull IntoResponse<?> handleReloadRequest(
    @NonNull @Optional @FirstRequestQueryParam(value = "type", def = "all") String type
  ) {
    switch (StringUtil.toLower(type)) {
      case "all" -> {
        this.reloadConfig();
        this.moduleProvider.reloadAll();
      }
      case "config" -> this.reloadConfig();
      default -> {
        return ProblemDetail.builder()
          .type("invalid-reload-type")
          .title("Invalid Reload Type")
          .status(HttpResponseCode.BAD_REQUEST)
          .detail(String.format(
            "The requested reload type %s is not supported. Supported values are: all, config",
            type));
      }
    }

    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/node/liveConsole")
  public @NonNull IntoResponse<?> handleLiveConsoleRequest(
    @NonNull HttpContext context,
    @Authentication(
      providers = {"ticket", "jwt"},
      scopes = {"cloudnet_rest:node_read", "cloudnet_rest:node_live_console"}) @NonNull RestUser restUser
  ) {
    if (this.logger instanceof ch.qos.logback.classic.Logger logbackLogger) {
      context.upgrade().thenAccept(channel -> {
        var webSocketAppender = new WebSocketLogAppender(logbackLogger, restUser, channel);
        var appender = logbackLogger.getAppender("Rolling");
        if (appender instanceof OutputStreamAppender<ILoggingEvent> consoleAppender) {
          webSocketAppender.setEncoder(consoleAppender.getEncoder());
        }

        logbackLogger.addAppender(webSocketAppender);
        channel.addListener(webSocketAppender);
      });
    } else {
      return ProblemDetail.builder()
        .type("console-unsupported-logger")
        .title("Console Unsupported Logger")
        .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
        .detail("The console logger is not using a supported logback logging implementation.");
    }

    return HttpResponseCode.SWITCHING_PROTOCOLS;
  }

  private void reloadConfig() {
    this.configuration.reloadFrom(JsonConfiguration.loadFromFile());
    this.serviceTaskProvider.reload();
    this.groupConfigurationProvider.reload();
  }

  protected class WebSocketLogAppender extends ConsoleAppender<ILoggingEvent> implements WebSocketListener {

    protected final ch.qos.logback.classic.Logger logger;
    protected final RestUser user;
    protected final WebSocketChannel channel;

    public WebSocketLogAppender(
      @NonNull ch.qos.logback.classic.Logger logger,
      @NonNull RestUser user,
      @NonNull WebSocketChannel channel
    ) {
      this.logger = logger;
      this.user = user;
      this.channel = channel;
    }

    @Override
    public void handle(@NonNull WebSocketChannel channel, @NonNull WebSocketFrameType type, byte[] bytes) {
      if (type == WebSocketFrameType.TEXT) {
        if (this.user.hasScope("cloudnet_rest:node_send_commands")) {
          var commandLine = new String(bytes, StandardCharsets.UTF_8);
          var commandSource = new DriverCommandSource();
          TaskUtil.getOrDefault(V3HttpHandlerNode.this.commandProvider.execute(commandSource, commandLine), null);

          for (var message : commandSource.messages()) {
            this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, message);
          }
        }
      }
    }

    @Override
    public void handleClose(
      @NonNull WebSocketChannel channel,
      @NonNull AtomicInteger statusCode,
      @NonNull AtomicReference<String> statusText
    ) {
      this.logger.detachAppender(this);
    }

    @Override
    protected void append(@NonNull ILoggingEvent event) {
      this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, this.encoder.encode(event));
    }
  }
}
