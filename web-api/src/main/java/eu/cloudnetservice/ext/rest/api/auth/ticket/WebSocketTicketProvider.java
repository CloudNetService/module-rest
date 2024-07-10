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

package eu.cloudnetservice.ext.rest.api.auth.ticket;

import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import java.util.Set;
import lombok.NonNull;

public interface WebSocketTicketProvider<T> {

  /**
   * The default provider priority that any base implementation should use.
   */
  int DEFAULT_PRIORITY = 0;

  /**
   * The priority of this provider. During registration providers with a higher priority will override providers with
   * the same name but a lower priority. In case the same priority is used by two providers with the same name, the
   * handling falls back to natural sorting.
   *
   * @return the priority to use for this provider during registration.
   * @see #DEFAULT_PRIORITY
   */
  int priority();

  /**
   * Get the name of this provider. Internally the result of this method is always converted to lower case, no other
   * constraints apply.
   *
   * @return the name of this auth provider.
   */
  @NonNull
  String name();

  @NonNull
  WebSocketTicketResult generateWebSocketTicket(@NonNull RestUser restUser, @NonNull Set<String> requestedScopes);

  boolean verifyWebSocketTicket(@NonNull RestUser restUser, @NonNull WebSocketTicket<T> ticket);

}
