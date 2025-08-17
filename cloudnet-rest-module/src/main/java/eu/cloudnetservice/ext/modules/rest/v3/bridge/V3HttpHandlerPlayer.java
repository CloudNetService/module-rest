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

package eu.cloudnetservice.ext.modules.rest.v3.bridge;

import com.google.common.base.Enums;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.ext.modules.rest.page.PageSortingMode;
import eu.cloudnetservice.ext.modules.rest.page.Paging;
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
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import eu.cloudnetservice.utils.base.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@EnableValidation
public class V3HttpHandlerPlayer {

  private final PlayerManager playerManager;

  @Inject
  public V3HttpHandlerPlayer(@Service PlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  @RequestHandler(path = "/api/v3/player/onlineCount")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read", "cloudnet_bridge:player_online_count"})
  public @NonNull IntoResponse<?> handleOnlineCount() {
    return JsonResponse.builder().body(Map.of("onlineCount", this.playerManager.onlineCount()));
  }

  @RequestHandler(path = "/api/v3/player/registeredCount")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read",
    "cloudnet_bridge:player_registered_count"})
  public @NonNull IntoResponse<?> handleRegisteredCount() {
    return JsonResponse.builder().body(Map.of("registeredCount", this.playerManager.registeredCount()));
  }

  @RequestHandler(path = "/api/v3/player/online")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read", "cloudnet_bridge:player_get_bulk"})
  public @NonNull IntoResponse<?> handleOnlinePlayerList(
    @Optional @Valid @FirstRequestQueryParam(value = "limit", def = "10") String limit,
    @Optional @Valid @FirstRequestQueryParam(value = "offset", def = "0") String offset,
    @Optional @FirstRequestQueryParam(value = "sort", def = "asc") String sort
  ) {
    var limitInt = Ints.tryParse(limit);
    if (limitInt == null || limitInt < 1 || limitInt > 25) {
      return ProblemDetail.builder()
        .type("malformed-limit-parameter")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Limit Query Parameter")
        .detail("The provided limit query parameter is not a valid integer.");
    }

    var offsetInt = Ints.tryParse(offset);
    if (offsetInt == null || offsetInt < 0) {
      return ProblemDetail.builder()
        .type("malformed-offset-parameter")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Offset Query Parameter")
        .detail("The provided offset query parameter is not a valid integer.");
    }

    var sortingMode = PageSortingMode.findSortingMode(sort);
    if (sortingMode == null) {
      return ProblemDetail.builder()
        .type("malformed-sort-parameter")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Sort Query Parameter")
        .detail("The provided sort query parameter '%s' is unknown".formatted(sort));
    }

    // create the correct slice for our data based on the input
    return Paging.pagedJsonResponse(
      "onlinePlayers",
      this.playerManager.onlinePlayers().players(),
      CloudOfflinePlayer::name,
      sortingMode,
      limitInt,
      offsetInt);
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read", "cloudnet_bridge:player_get"})
  public @NonNull IntoResponse<?> handleOnlinePlayer(@NonNull @RequestPathParam("identifier") String identifier) {
    return this.handleCloudPlayerContext(identifier, player -> JsonResponse.builder().body(player));
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/exists")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read", "cloudnet_bridge:player_exists"})
  public @NonNull IntoResponse<?> handleOnlinePlayerExists(@NonNull @RequestPathParam("identifier") String identifier) {
    return this.handleCloudPlayerContext(identifier, $ -> HttpResponseCode.NO_CONTENT);
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/connectService", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_bridge:player_write", "cloudnet_bridge:player_connect_service"})
  public @NonNull IntoResponse<?> handleOnlinePlayerConnectService(
    @NonNull @RequestPathParam("identifier") String identifier,
    @NonNull @FirstRequestQueryParam("target") String target
  ) {
    return this.handlePlayerExecutorContext(identifier, playerExecutor -> playerExecutor.connect(target));
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/connect", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_bridge:player_write", "cloudnet_bridge:player_connect_group_task"})
  public @NonNull IntoResponse<?> handleOnlinePlayerConnect(
    @NonNull @RequestPathParam("identifier") String identifier,
    @NonNull @FirstRequestQueryParam("serverSelector") String serverSelector,
    @NonNull @FirstRequestQueryParam("target") String target,
    @NonNull @FirstRequestQueryParam("type") String type
  ) {
    var selector = Enums.getIfPresent(ServerSelectorType.class, serverSelector);
    if (!selector.isPresent()) {
      return ProblemDetail.builder()
        .type("malformed-server-selector")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Server Selector")
        .detail("Malformed server selector. Selector %s is unknown.".formatted(selector));
    }

    var connectType = ConnectType.fromName(type);
    if (connectType == null) {
      return ProblemDetail.builder()
        .type("malformed-connect-type")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Connect Type")
        .detail("Malformed connect type. Connect type %s unknown.".formatted(type));
    }

    return this.handlePlayerExecutorContext(identifier, playerExecutor -> {
      switch (connectType) {
        case TASK -> playerExecutor.connectToTask(target, selector.get());
        case GROUP -> playerExecutor.connectToGroup(target, selector.get());
      }
    });
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/connectFallback", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_bridge:player_write", "cloudnet_bridge:player_connect_fallback"})
  public @NonNull IntoResponse<?> handleOnlinePlayerConnectFallback(
    @NonNull @RequestPathParam("identifier") String identifier
  ) {
    return this.handlePlayerExecutorContext(identifier, PlayerExecutor::connectToFallback);
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/kick", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_bridge:player_write", "cloudnet_bridge:player_disconnect"})
  public @NonNull IntoResponse<?> handleOnlinePlayerKick(
    @NonNull @RequestPathParam("identifier") String identifier,
    @NonNull @RequestTypedBody Map<String, String> message
  ) {
    var kickMessage = message.get("kickMessage");
    if (kickMessage == null) {
      return ProblemDetail.builder()
        .type("malformed-kick-message")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Kick Message")
        .detail("Could not extract kick message from request body.");
    }

    return this.handlePlayerExecutorContext(
      identifier,
      playerExecutor -> playerExecutor.kick(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(kickMessage)));
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/sendChat", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_bridge:player_write", "cloudnet_bridge:player_send_chat"})
  public @NonNull IntoResponse<?> handleOnlinePlayerChat(
    @NonNull @RequestPathParam("identifier") String identifier,
    @NonNull @RequestTypedBody Map<String, String> message
  ) {
    var chatMessage = message.get("chatMessage");
    if (chatMessage == null) {
      return ProblemDetail.builder()
        .type("malformed-chat-message")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Chat Message")
        .detail("Could not extract chat message from request body.");
    }

    return this.handlePlayerExecutorContext(
      identifier,
      playerExecutor -> playerExecutor.sendChatMessage(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(chatMessage)));
  }

  @RequestHandler(path = "/api/v3/player/online/{identifier}/command", method = HttpMethod.POST)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_bridge:player_write", "cloudnet_bridge:player_send_command"})
  public @NonNull IntoResponse<?> handleOnlinePlayerCommand(
    @NonNull @RequestPathParam("identifier") String identifier,
    @NonNull @RequestTypedBody Map<String, String> message,
    @NonNull @FirstRequestQueryParam("redirectToServer") String redirectToServer
  ) {
    var command = message.get("command");
    if (command == null) {
      return ProblemDetail.builder()
        .type("malformed-command")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Command")
        .detail("Could not extract command from request body.");
    }

    return this.handlePlayerExecutorContext(
      identifier,
      playerExecutor -> playerExecutor.spoofCommandExecution(command, Boolean.parseBoolean(redirectToServer)));
  }

  private @NonNull IntoResponse<?> handlePlayerExecutorContext(
    @NonNull String identifier,
    @NonNull Consumer<PlayerExecutor> executor
  ) {
    return this.handleCloudPlayerContext(identifier, cloudPlayer -> {
      executor.accept(this.playerManager.playerExecutor(cloudPlayer.uniqueId()));
      return HttpResponseCode.NO_CONTENT;
    });
  }

  private @NonNull IntoResponse<?> handleCloudPlayerContext(
    @NonNull String identifier,
    @NonNull Function<CloudPlayer, IntoResponse<?>> mapper
  ) {
    CloudPlayer cloudPlayer;
    try {
      var uniqueId = UUID.fromString(identifier);
      cloudPlayer = this.playerManager.onlinePlayer(uniqueId);
    } catch (IllegalArgumentException exception) {
      cloudPlayer = this.playerManager.firstOnlinePlayer(identifier);
    }

    if (cloudPlayer == null) {
      return ProblemDetail.builder()
        .type("player-not-found")
        .status(HttpResponseCode.NOT_FOUND)
        .title("No player found")
        .detail("No online player with the identifier %s found".formatted(identifier));
    }

    return mapper.apply(cloudPlayer);
  }

  enum ConnectType {
    TASK,
    GROUP;

    public static @Nullable ConnectType fromName(@NonNull String type) {
      return switch (StringUtil.toLower(type)) {
        case "task" -> TASK;
        case "group" -> GROUP;
        default -> null;
      };
    }
  }

}
