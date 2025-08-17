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

package eu.cloudnetservice.ext.rest.api.registry;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.api.response.type.PlainTextResponse;
import eu.cloudnetservice.ext.rest.api.tree.DynamicHttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.StaticHttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.WildcardPathNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public final class HttpHandlerRegistryTest {

  private static final HttpHandler EMPTY_HTTP_HANDLER = context -> PlainTextResponse.builder();
  private static final ComponentConfig EMPTY_COMPONENT_CONFIG = ComponentConfig.builder()
    .executorService(Executors.newVirtualThreadPerTaskExecutor())
    .build();

  @Mock
  private HttpContext httpContext;
  private HttpHandlerRegistry registry;

  private void setupRequestMock(Consumer<HttpRequest> requestDecorator) {
    var mockedRequest = Mockito.mock(HttpRequest.class);
    requestDecorator.accept(mockedRequest);
    Mockito.when(this.httpContext.request()).thenReturn(mockedRequest);
  }

  @BeforeEach
  void constructRegistry() {
    this.registry = new DefaultHttpHandlerRegistry(EMPTY_COMPONENT_CONFIG);
  }

  @Test
  void testRootPath() {
    var httpContext = Mockito.mock(HttpContext.class);
    var registry = new DefaultHttpHandlerRegistry(EMPTY_COMPONENT_CONFIG);

    Assertions.assertDoesNotThrow(() -> registry.registerHandler(
      "/",
      EMPTY_HTTP_HANDLER,
      HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build()));
    Assertions.assertDoesNotThrow(() -> registry.registerHandler(
      "",
      EMPTY_HTTP_HANDLER,
      HttpHandlerConfig.builder().httpMethod(HttpMethod.POST).build()));

    Assertions.assertEquals(2, registry.registeredHandlers().size());

    Assertions.assertNotNull(registry.findHandler("/", httpContext));
    Assertions.assertNotNull(registry.findHandler("//", httpContext));
    Assertions.assertNotNull(registry.findHandler("///", httpContext));
    Assertions.assertNotNull(registry.findHandler("", httpContext));

    Assertions.assertNull(registry.findHandler("/world", httpContext));
    Assertions.assertNull(registry.findHandler("////////", httpContext));
  }

  @Test
  void testDynamic() {
    Map<String, String> pathParameters = Mockito.spy(new HashMap<>());
    this.setupRequestMock(request -> Mockito.when(request.pathParameters()).thenReturn(pathParameters));

    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/{name}", EMPTY_HTTP_HANDLER, config));
    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/{name}/kick", EMPTY_HTTP_HANDLER, config));
    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/{name}/ban", EMPTY_HTTP_HANDLER, config));

    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/{name}", EMPTY_HTTP_HANDLER, config));
    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/{other}", EMPTY_HTTP_HANDLER, config));
    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/{}", EMPTY_HTTP_HANDLER, config));

    Assertions.assertEquals(3, this.registry.registeredHandlers().size());

    Assertions.assertNotNull(this.registry.findHandler("api/test/playo", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("api/test/playo/kick", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("api/test/playo/ban", this.httpContext));

    Assertions.assertNull(this.registry.findHandler("api/test/playo/other", this.httpContext));
    Assertions.assertNull(this.registry.findHandler("api/v2/test/playo", this.httpContext));

    Mockito.verify(pathParameters, Mockito.times(4)).put("name", "playo");
    Assertions.assertEquals("playo", pathParameters.get("name"));
  }

  @Test
  void testStatic() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/static/", EMPTY_HTTP_HANDLER, config));
    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/static/void/", EMPTY_HTTP_HANDLER, config));
    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("/api/test/google/", EMPTY_HTTP_HANDLER, config));

    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/google/", EMPTY_HTTP_HANDLER, config));

    Assertions.assertEquals(3, this.registry.registeredHandlers().size());

    Assertions.assertNotNull(this.registry.findHandler("api/test/static", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("api/test/STATIC", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("api/test/static/void", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("api/test/google", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("/api/test/google", this.httpContext));
    Assertions.assertNotNull(this.registry.findHandler("/Api/TeSt/GooGle/", this.httpContext));

    Assertions.assertNull(this.registry.findHandler("/api/test/static/unknown", this.httpContext));
    Assertions.assertNull(this.registry.findHandler("api/test/static/other", this.httpContext));
  }

  @Test
  void testWildcard() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/*", EMPTY_HTTP_HANDLER, config));
    Assertions.assertDoesNotThrow(
      () -> this.registry.registerHandler("api/test/wildcard/*", EMPTY_HTTP_HANDLER, config));

    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/*", EMPTY_HTTP_HANDLER, config));
    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/*/world", EMPTY_HTTP_HANDLER, config));

    Assertions.assertEquals(2, this.registry.registeredHandlers().size());

    var deepHandlerA = this.registry.findHandler("api/test/wildcard", this.httpContext);
    Assertions.assertNotNull(deepHandlerA);
    Assertions.assertEquals("/ -> api -> test -> wildcard", deepHandlerA.treePath());

    var deepHandlerB = this.registry.findHandler("api/test/wildcard/hello/world/", this.httpContext);
    Assertions.assertNotNull(deepHandlerB);
    Assertions.assertEquals("/ -> api -> test -> wildcard -> *", deepHandlerB.treePath());

    var deepHandlerC = this.registry.findHandler("api/test", this.httpContext);
    Assertions.assertNotNull(deepHandlerC);
    Assertions.assertEquals("/ -> api -> test", deepHandlerC.treePath());

    var deepHandlerD = this.registry.findHandler("api/test/hello/world/123^3", this.httpContext);
    Assertions.assertNotNull(deepHandlerD);
    Assertions.assertEquals("/ -> api -> test -> *", deepHandlerD.treePath());
  }

  @Test
  void testMultipleHandlerRegister() {
    Assertions.assertDoesNotThrow(() -> this.registry.registerHandler(
      "/api/v3/test",
      context -> PlainTextResponse.builder(),
      HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build()));
    Assertions.assertDoesNotThrow(() -> this.registry.registerHandler(
      "/api/v3/test",
      context -> JsonResponse.builder(),
      HttpHandlerConfig.builder().httpMethod(HttpMethod.POST).build()));
    Assertions.assertDoesNotThrow(() -> this.registry.registerHandler(
      "/api/v3/test",
      context -> PlainTextResponse.builder(),
      HttpHandlerConfig.builder().httpMethod(HttpMethod.DELETE).build()));

    Assertions.assertEquals(3, this.registry.registeredHandlers().size());

    var treeNode = this.registry.findHandler("/api/v3/test", this.httpContext);
    Assertions.assertNotNull(treeNode);

    var pathNode = treeNode.pathNode();
    Assertions.assertInstanceOf(StaticHttpPathNode.class, pathNode);
    Assertions.assertEquals(3, pathNode.handlers().size());

    var getHandler = pathNode.findHandlerForMethod(HttpMethod.GET.name());
    Assertions.assertNotNull(getHandler);

    var postHandler = pathNode.findHandlerForMethod(HttpMethod.POST.name());
    Assertions.assertNotNull(postHandler);
    Assertions.assertNotSame(getHandler, postHandler);

    var deleteHandler = pathNode.findHandlerForMethod(HttpMethod.DELETE.name());
    Assertions.assertNotNull(deleteHandler);
    Assertions.assertNotSame(getHandler, deleteHandler);
    Assertions.assertNotSame(postHandler, deleteHandler);

    Assertions.assertTrue(pathNode.unregisterMatchingHandler(pair -> pair.config().httpMethod() == HttpMethod.DELETE));
    var deleteHandlerB = pathNode.findHandlerForMethod(HttpMethod.DELETE.name());
    Assertions.assertNull(deleteHandlerB);

    Assertions.assertEquals(2, this.registry.registeredHandlers().size());
  }

  @Test
  void testDynamicWildcardPathMix() {
    // this test specifically ensures that if a dynamic node path was taken and later down the tree we realize
    // that the path cannot work, that the dynamic elements are removed from the path before returning the wildcard
    // handler - this ensures that the wildcard handler does not receive parameters that are irrelevant (or simply wrong)
    //
    // in other words: this test is checking if the path parameter is unregistered in case the go over a dynamic node
    // but end up returning a wildcard node due to a missing handler further down the tree:
    //  1. '/api/service/Lobby-1/stop/c': actual registered path, parameter 'name' should be registered
    //  2. '/api/service/Lobby-1/stop/delete': path is not registered but the wildcard at '/api/service/*' matches -
    //                                         the path parameter 'name' should not be registered

    // pre-fill the tree in the following way:
    //                /api
    //      player      |             service
    //                  *   |          {name}           |    list
    //                          start       |      stop
    //                                          c   |   b
    var handlerConfig = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();
    this.registry.registerHandler("/api/player", EMPTY_HTTP_HANDLER, handlerConfig);
    this.registry.registerHandler("/api/service/*", EMPTY_HTTP_HANDLER, handlerConfig);
    this.registry.registerHandler("/api/service/list", EMPTY_HTTP_HANDLER, handlerConfig);
    this.registry.registerHandler("/api/service/{name}/start", EMPTY_HTTP_HANDLER, handlerConfig);
    this.registry.registerHandler("/api/service/{name}/stop/c", EMPTY_HTTP_HANDLER, handlerConfig);
    this.registry.registerHandler("/api/service/{name}/stop/b", EMPTY_HTTP_HANDLER, handlerConfig);

    var serviceListNode = this.registry.findHandler("/api/service/list", this.httpContext);
    Assertions.assertNotNull(serviceListNode);
    Assertions.assertEquals(1, serviceListNode.pathNode().handlerCount());

    Map<String, String> pathParametersForActualStopCall = Mockito.spy(new HashMap<>());
    this.setupRequestMock(req -> Mockito.when(req.pathParameters()).thenReturn(pathParametersForActualStopCall));
    var stopCNode = this.registry.findHandler("/api/service/Lobby-1/stop/c", this.httpContext);
    Assertions.assertNotNull(stopCNode);
    Assertions.assertEquals(1, stopCNode.pathNode().handlerCount());
    Assertions.assertEquals("/ -> api -> service -> {name} -> stop -> c", stopCNode.treePath());
    Mockito.verify(pathParametersForActualStopCall, Mockito.atMostOnce()).put(Mockito.eq("name"), Mockito.anyString());
    Assertions.assertEquals("Lobby-1", pathParametersForActualStopCall.get("name"));
    Assertions.assertEquals(1, pathParametersForActualStopCall.size());

    Map<String, String> parametersForWildcardCall = Mockito.spy(new HashMap<>());
    this.setupRequestMock(req -> Mockito.when(req.pathParameters()).thenReturn(parametersForWildcardCall));
    var wildCardNode = this.registry.findHandler("/api/service/Lobby-1/stop/delete", this.httpContext);
    Assertions.assertNotNull(wildCardNode);
    Assertions.assertEquals(1, wildCardNode.pathNode().handlerCount());
    Assertions.assertEquals("/ -> api -> service -> *", wildCardNode.treePath());
    Mockito.verify(pathParametersForActualStopCall, Mockito.atMostOnce()).put(Mockito.eq("name"), Mockito.anyString());
    Assertions.assertFalse(parametersForWildcardCall.containsKey("name"));
    Assertions.assertTrue(parametersForWildcardCall.isEmpty());
  }

  @Test
  void testPriority() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();
    this.setupRequestMock(request -> Mockito.when(request.pathParameters()).thenReturn(new HashMap<>()));

    this.registry.registerHandler("api/test/static", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("api/test/{static}", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("api/test/*", EMPTY_HTTP_HANDLER, config);

    var staticResult = this.registry.findHandler("api/test/static", this.httpContext);
    Assertions.assertNotNull(staticResult);
    Assertions.assertInstanceOf(StaticHttpPathNode.class, staticResult.pathNode());

    var dynamicResult = this.registry.findHandler("api/test/other", this.httpContext);
    Assertions.assertNotNull(dynamicResult);
    Assertions.assertInstanceOf(DynamicHttpPathNode.class, dynamicResult.pathNode());

    var wildcardResult = this.registry.findHandler("api/test/static/more", this.httpContext);
    Assertions.assertNotNull(wildcardResult);
    Assertions.assertInstanceOf(WildcardPathNode.class, wildcardResult.pathNode());
  }

  @Test
  void testDynamicNodeWithRegex() {
    Map<String, String> pathParameters = new HashMap<>();
    this.setupRequestMock(request -> Mockito.when(request.pathParameters()).thenReturn(pathParameters));

    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("/num/{number};\\d+", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("/lower/{string};[a-z]+", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("/insensitive/{string};[a-z]+;i", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("/semicolon/{string};@[a-z;]+;g", EMPTY_HTTP_HANDLER, config);

    Assertions.assertThrows(
      PatternSyntaxException.class,
      () -> this.registry.registerHandler("/invalid/{string};@[a-z+;g", EMPTY_HTTP_HANDLER, config));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> DynamicHttpPathNode.parse("name", "[a-z]+", "z"));

    var numberNode = this.registry.findHandler("/num/12345", this.httpContext);
    Assertions.assertNotNull(numberNode);
    Assertions.assertEquals("12345", pathParameters.get("number"));

    var invalidNumberNode = this.registry.findHandler("/num/abc123", this.httpContext);
    Assertions.assertNull(invalidNumberNode);

    var lowerStringNode = this.registry.findHandler("/lower/abcde", this.httpContext);
    Assertions.assertNotNull(lowerStringNode);
    Assertions.assertEquals("abcde", pathParameters.get("string"));

    var invalidLowerStringNode = this.registry.findHandler("/lower/Abcd", this.httpContext);
    Assertions.assertNull(invalidLowerStringNode);

    var insensitiveNode = this.registry.findHandler("/insensitive/ABcDEFgHi", this.httpContext);
    Assertions.assertNotNull(insensitiveNode);
    Assertions.assertEquals("ABcDEFgHi", pathParameters.get("string"));

    var invalidInsensitiveNode = this.registry.findHandler("/insensitive/1AdbCDef", this.httpContext);
    Assertions.assertNull(invalidInsensitiveNode);

    var semicolonNode = this.registry.findHandler("/semicolon/@abc;ef@ef", this.httpContext);
    Assertions.assertNotNull(semicolonNode);
    Assertions.assertEquals("@abc;ef@ef", pathParameters.get("string"));

    var invalidSemicolonNode = this.registry.findHandler("/semicolon/abc;Ef", this.httpContext);
    Assertions.assertNull(invalidSemicolonNode);
  }

  @Test
  void testUnregister() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    HttpHandler handlerA = context -> PlainTextResponse.builder();
    HttpHandler handlerB = context -> JsonResponse.builder();

    this.registry.registerHandler("api/hello/world", handlerA, config);
    this.registry.registerHandler("api/v2/testing", handlerB, config);

    Assertions.assertEquals(2, this.registry.registeredHandlers().size());

    var treeNode = this.registry.findHandler("api/hello/world", this.httpContext);
    Assertions.assertNotNull(treeNode);

    var handlerPair = treeNode.pathNode().findHandlerForMethod(HttpMethod.GET.name());
    Assertions.assertNotNull(handlerPair);
    Assertions.assertSame(handlerA, handlerPair.httpHandler());

    this.registry.unregisterHandler(handlerPair.httpHandler());
    Assertions.assertNull(this.registry.findHandler("api/hello/world", this.httpContext));
    Assertions.assertEquals(1, this.registry.registeredHandlers().size());
  }

  @Test
  void testClassLoaderUnregister() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/hello/world", EMPTY_HTTP_HANDLER, config);
    Assertions.assertNotNull(this.registry.findHandler("api/hello/world", this.httpContext));
    Assertions.assertEquals(1, this.registry.registeredHandlers().size());

    this.registry.unregisterHandlers(this.getClass().getClassLoader());
    Assertions.assertNull(this.registry.findHandler("api/hello/world", this.httpContext));
    Assertions.assertEquals(0, this.registry.registeredHandlers().size());
  }

  @Test
  public void testClear() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/hello/world", EMPTY_HTTP_HANDLER, config);
    Assertions.assertNotNull(this.registry.findHandler("api/hello/world", this.httpContext));
    Assertions.assertEquals(1, this.registry.registeredHandlers().size());

    this.registry.clearHandlers();
    Assertions.assertNull(this.registry.findHandler("api/hello/world", this.httpContext));
    Assertions.assertEquals(0, this.registry.registeredHandlers().size());
  }
}
