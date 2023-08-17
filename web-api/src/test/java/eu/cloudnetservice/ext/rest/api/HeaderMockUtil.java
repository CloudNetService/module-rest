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

package eu.cloudnetservice.ext.rest.api;

import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import org.mockito.Mockito;

public final class HeaderMockUtil {

  public static HttpContext setupContext(HttpHeaderMap httpHeaderMap) {
    var context = Mockito.mock(HttpContext.class);
    var request = Mockito.mock(HttpRequest.class);
    Mockito.when(context.request()).thenReturn(request);
    Mockito.when(request.headers()).thenReturn(httpHeaderMap);

    return context;
  }

}
