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

package eu.cloudnetservice.ext.modules.rest.v3;

import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import lombok.NonNull;

@Singleton
public final class V3HttpHandlerDatabase {

  private final DatabaseProvider databaseProvider;

  @Inject
  public V3HttpHandlerDatabase(@NonNull DatabaseProvider databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @RequestHandler(path = "/api/v3/database")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_list"})
  public @NonNull IntoResponse<?> handleNamesRequest() {
    return JsonResponse.builder().body(Map.of("databaseNames", this.databaseProvider.databaseNames()));
  }

  @RequestHandler(path = "/api/v3/database/{name}/clear", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_list"})
  public @NonNull IntoResponse<?> handleClearRequest(@NonNull @RequestPathParam("name") String name) {
    this.databaseProvider.database(name).clear();
    return HttpResponseCode.NO_CONTENT;
  }

  @RequestHandler(path = "/api/v3/database/{name}/contains")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_contains"})
  public @NonNull IntoResponse<?> handleContainsRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("key") String key
  ) {
    var database = this.databaseProvider.database(name);
    return JsonResponse.builder().body(Map.of("result", database.contains(key)));
  }

  @RequestHandler(path = "/api/v3/database/{name}/keys")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_keys"})
  public @NonNull IntoResponse<?> handleKeysRequest(@NonNull @RequestPathParam("name") String name) {
    var database = this.databaseProvider.database(name);
    return JsonResponse.builder().body(Map.of("keys", database.keys()));
  }

  @RequestHandler(path = "/api/v3/database/{name}/count")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_count"})
  public @NonNull IntoResponse<?> handleCountRequest(@NonNull @RequestPathParam("name") String name) {
    var database = this.databaseProvider.database(name);
    return JsonResponse.builder().body(Map.of("count", database.documentCount()));
  }

  @RequestHandler(path = "/api/v3/database/{name}", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_write", "cloudnet_rest:database_insert"})
  public @NonNull IntoResponse<?> handleInsert(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestTypedBody Document document
  ) {
    var database = this.databaseProvider.database(name);
    var key = document.getString("key");
    var data = document.readDocument("document");

    if (key == null) {
      return ProblemDetail.builder()
        .type("missing-database-key")
        .title("Missing Database Key")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body has no database key to insert the data to.");
    }

    if (database.insert(key, data)) {
      return HttpResponseCode.CREATED;
    } else {
      return ProblemDetail.builder()
        .type("database-insert-failed")
        .title("Database Insert Failed")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail(String.format("The database could not associate the key %s with the given value.", key));
    }
  }

  @RequestHandler(path = "/api/v3/database/{name}/get")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_get"})
  public @NonNull IntoResponse<?> handleGetRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("key") String key
  ) {
    var database = this.databaseProvider.database(name);
    return JsonResponse.builder().body(DocumentFactory.json().newDocument("result", database.get(key)));
  }

  @RequestHandler(path = "/api/v3/database/{name}/find")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_read", "cloudnet_rest:database_find"})
  public @NonNull IntoResponse<?> handleFindRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestTypedBody Map<String, String> filter
  ) {
    var database = this.databaseProvider.database(name);
    return JsonResponse.builder().body(database.find(filter));
  }

  @RequestHandler(path = "/api/v3/database/{name}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:database_write", "cloudnet_rest:database_delete"})
  public @NonNull IntoResponse<?> handleDeleteRequest(
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("key") String key
  ) {
    var database = this.databaseProvider.database(name);
    if (database.delete(key)) {
      return HttpResponseCode.NO_CONTENT;
    }

    return ProblemDetail.builder()
      .status(HttpResponseCode.OK)
      .type("database-delete-failed")
      .title("Database Delete Failed")
      .detail("The database had nothing to delete. The key was not associated with any data.");
  }
}
