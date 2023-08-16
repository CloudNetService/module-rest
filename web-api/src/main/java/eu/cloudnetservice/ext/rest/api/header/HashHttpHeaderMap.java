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

package eu.cloudnetservice.ext.rest.api.header;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

final class HashHttpHeaderMap implements HttpHeaderMap {

  private final SetMultimap<String, String> headers;

  public HashHttpHeaderMap(int sizeHint) {
    this(LinkedHashMultimap.create(sizeHint, 4));
  }

  private HashHttpHeaderMap(@NonNull SetMultimap<String, String> headers) {
    this.headers = headers;
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public @NonNull HttpHeaderMap clone() {
    return new HashHttpHeaderMap(LinkedHashMultimap.create(this.headers));
  }

  @Override
  public @NonNull HttpHeaderMap unmodifiableClone() {
    return new HashHttpHeaderMap(ImmutableSetMultimap.copyOf(this.headers));
  }

  @Override
  public int size() {
    return this.headers.size();
  }

  @Override
  public boolean contains(@NonNull String headerName) {
    return this.headers.containsKey(headerName);
  }

  @Override
  public boolean contains(@NonNull String headerName, @NonNull String headerValue) {
    return this.headers.containsEntry(headerName, headerValue);
  }

  @Override
  public @Nullable String firstValue(@NonNull String headerName) {
    return this.firstValue(headerName, null);
  }

  @Override
  public @UnknownNullability String firstValue(@NonNull String headerName, @Nullable String defaultValue) {
    var headerValues = this.headers.get(headerName);
    return headerValues.isEmpty() ? null : Iterables.getFirst(headerValues, defaultValue);
  }

  @Override
  public @NonNull Collection<String> values(@NonNull String headerName) {
    return this.headers.get(headerName);
  }

  @Override
  public @NonNull Set<String> names() {
    return this.headers.keySet();
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull HttpHeaderMap headerMap) {
    for (var entry : headerMap) {
      this.add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull Map<String, ? extends Iterable<String>> headers) {
    for (var entry : headers.entrySet()) {
      this.add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String headerValue) {
    this.headers.put(headerName, headerValue);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String... headerValues) {
    for (String headerValue : headerValues) {
      this.add(headerName, headerValue);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    headerValues.forEach(headerValue -> this.add(headerName, headerValue));
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull HttpHeaderMap headerMap) {
    for (var entry : headerMap) {
      this.set(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull Map<String, ? extends Iterable<String>> headers) {
    for (var entry : headers.entrySet()) {
      this.set(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String headerValue) {
    this.headers.replaceValues(headerName, Set.of(headerValue));
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String... headerValues) {
    this.headers.replaceValues(headerName, List.of(headerValues));
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    this.headers.replaceValues(headerName, headerValues);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String headerValue) {
    if (!this.headers.containsKey(headerName)) {
      return this.set(headerName, headerValue);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String... headerValues) {
    if (!this.headers.containsKey(headerName)) {
      return this.set(headerName, headerValues);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    if (!this.headers.containsKey(headerName)) {
      return this.set(headerName, headerValues);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap clear() {
    this.headers.clear();
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap remove(@NonNull String headerName) {
    this.headers.removeAll(headerName);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap remove(@NonNull String headerName, @NonNull String headerValue) {
    this.headers.remove(headerName, headerValue);
    return this;
  }

  @Override
  public @NonNull Stream<Map.Entry<String, String>> stream() {
    return this.entries().stream();
  }

  @Override
  public @NonNull Collection<Map.Entry<String, String>> entries() {
    return this.headers.entries();
  }

  @Override
  public @NonNull Map<String, ? extends Collection<String>> asMap() {
    return this.headers.asMap();
  }

  @Override
  public @NonNull Iterator<Map.Entry<String, String>> iterator() {
    return this.entries().iterator();
  }

  @Override
  public @NonNull Spliterator<Map.Entry<String, String>> spliterator() {
    return this.entries().spliterator();
  }
}
