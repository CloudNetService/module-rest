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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.ext.modules.rest.dto.NetworkClusterNodeDto;
import eu.cloudnetservice.ext.modules.rest.validation.TrueFalse;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import eu.cloudnetservice.node.cluster.LocalNodeServer;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.config.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@EnableValidation
public final class V3HttpHandlerCluster {

  private static final ProblemDetail MISSING_CLUSTER_NODE = ProblemDetail.builder()
    .type("missing-cluster-node")
    .status(HttpResponseCode.BAD_REQUEST)
    .title("Missing Cluster Node Information")
    .detail("The request body does not contain a cluster node.")
    .build();

  private final Configuration configuration;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public V3HttpHandlerCluster(@NonNull Configuration configuration, @NonNull NodeServerProvider nodeServerProvider) {
    this.configuration = configuration;
    this.nodeServerProvider = nodeServerProvider;
  }

  @RequestHandler(path = "/api/v3/cluster")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:cluster_read", "cloudnet_rest:cluster_node_list"})
  public @NonNull IntoResponse<?> handleNodeList() {
    var nodes = this.nodeServerProvider.nodeServers().stream().map(this::createNodeInfoDocument).toList();
    return JsonResponse.builder().body(Map.of("nodes", nodes));
  }

  @RequestHandler(path = "/api/v3/cluster/{node}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:cluster_read", "cloudnet_rest:cluster_node_list"})
  public @NonNull IntoResponse<?> handleNodeRequest(@NonNull @RequestPathParam("node") String node) {
    var server = this.nodeServerProvider.node(node);
    if (server != null) {
      return JsonResponse.builder().body(this.createNodeInfoDocument(server));
    }

    return this.nodeServerNotFound(node);
  }

  @EnableValidation
  @RequestHandler(path = "/api/v3/cluster/{node}/drain", method = HttpMethod.PATCH)
  @Authentication(
    providers = "jwt",
    scopes = {"cloudnet_rest:cluster_write", "cloudnet_rest:cluster_node_change_draining"})
  public @NonNull IntoResponse<?> handleNodeDrainRequest(
    @NonNull @RequestPathParam("node") String node,
    @Valid @TrueFalse @NonNull @FirstRequestQueryParam("draining") String drainingParam
  ) {
    var server = this.nodeServerProvider.node(node);
    if (server == null) {
      return this.nodeServerNotFound(node);
    }

    server.drain(Boolean.parseBoolean(drainingParam));
    return HttpResponseCode.ACCEPTED;
  }

  @RequestHandler(path = "/api/v3/cluster/{node}/command", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:cluster_write", "cloudnet_rest:cluster_node_command"})
  public @NonNull IntoResponse<?> handleNodeCommandRequest(
    @NonNull @RequestPathParam("node") String node,
    @NonNull @RequestTypedBody Map<String, String> body
  ) {
    var server = this.nodeServerProvider.node(node);
    if (server == null) {
      return this.nodeServerNotFound(node);
    }

    var command = body.get("command");
    if (command == null) {
      return ProblemDetail.builder()
        .title("Missing Command Line")
        .type("missing-command-line")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body has no 'command' field.");
    }

    return JsonResponse.builder().body(Map.of("lines", server.sendCommandLine(command)));
  }

  @RequestHandler(path = "/api/v3/cluster", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:cluster_write", "cloudnet_rest:cluster_node_create"})
  public @NonNull IntoResponse<?> handleNodeCreateRequest(
    @Nullable @RequestTypedBody @Valid NetworkClusterNodeDto nodeDto
  ) {
    if (nodeDto == null) {
      return MISSING_CLUSTER_NODE;
    }

    var node = nodeDto.toEntity();

    if (this.nodeServerProvider.node(node.uniqueId()) != null) {
      return ProblemDetail.builder()
        .status(HttpResponseCode.BAD_REQUEST)
        .type("cluster-node-already-registered")
        .title("Cluster Node Already Registered")
        .detail(String.format("The node server with the unique id %s is already registered", node.uniqueId()));
    }

    this.configuration.clusterConfig().nodes().add(node);
    this.configuration.save();
    this.nodeServerProvider.registerNode(node);

    return HttpResponseCode.CREATED;
  }

  @RequestHandler(path = "/api/v3/cluster/{node}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:cluster_write", "cloudnet_rest:cluster_node_delete"})
  public @NonNull IntoResponse<?> handleNodeDeleteRequest(@NonNull @RequestPathParam("node") String node) {
    var removed = this.configuration.clusterConfig().nodes().removeIf(cn -> cn.uniqueId().equals(node));
    if (removed) {
      this.configuration.save();
      this.nodeServerProvider.registerNodes(this.configuration.clusterConfig());
      return HttpResponseCode.NO_CONTENT;
    }

    return this.nodeServerNotFound(node);
  }

  @RequestHandler(path = "/api/v3/cluster", method = HttpMethod.PUT)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:cluster_write", "cloudnet_rest:cluster_node_update"})
  public @NonNull IntoResponse<?> handleNodeUpdateRequest(
    @Nullable @RequestTypedBody @Valid NetworkClusterNodeDto nodeDto
  ) {
    if (nodeDto == null) {
      return MISSING_CLUSTER_NODE;
    }

    var node = nodeDto.toEntity();

    // check if we are trying to update the local node
    if (this.nodeServerProvider.localNode().info().uniqueId().equals(node.uniqueId())) {
      return ProblemDetail.builder()
        .title("Local Node Server Update Not Allowed")
        .type("local-node-server-update-not-allowed")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail(String.format(
          "Requested node server %s is the local node server. Updating the local node server using this route is not allowed.",
          node.uniqueId()));
    }

    var registered = this.configuration.clusterConfig().nodes()
      .stream()
      .filter(clusterNode -> clusterNode.uniqueId().equals(node.uniqueId()))
      .findFirst()
      .orElse(null);
    if (registered == null) {
      return this.nodeServerNotFound(node.uniqueId());
    }

    this.configuration.clusterConfig().nodes().remove(registered);
    this.configuration.clusterConfig().nodes().add(node);
    this.configuration.save();

    this.nodeServerProvider.registerNodes(this.configuration.clusterConfig());
    return HttpResponseCode.NO_CONTENT;
  }

  private @NonNull ProblemDetail.Builder nodeServerNotFound(@NonNull String node) {
    return ProblemDetail.builder()
      .title("Node Server Not Found")
      .type("node-server-not-found")
      .status(HttpResponseCode.NOT_FOUND)
      .detail(String.format("Requested node server %s was not found.", node));
  }

  private @NonNull Document createNodeInfoDocument(@NonNull NodeServer node) {
    return Document.newJsonDocument()
      .append("node", node.info())
      .append("state", node.state())
      .append("head", node.head())
      .append("local", node instanceof LocalNodeServer)
      .append("nodeInfoSnapshot", node.nodeInfoSnapshot());
  }
}
