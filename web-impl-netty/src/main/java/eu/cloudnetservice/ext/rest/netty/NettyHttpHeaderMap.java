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

package eu.cloudnetservice.ext.rest.netty;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import io.netty5.handler.codec.http.headers.HttpHeaders;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

final class NettyHttpHeaderMap implements HttpHeaderMap {

  private final HttpHeaders httpHeaders;

  public NettyHttpHeaderMap(@NonNull HttpHeaders httpHeaders) {
    this.httpHeaders = httpHeaders;
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public @NonNull HttpHeaderMap clone() {
    return new NettyHttpHeaderMap(this.httpHeaders.copy());
  }

  @Override
  public @NonNull HttpHeaderMap unmodifiableClone() {
    // due to the conversion between CharSequence and String that is required for all collection
    // return types, there is no way to modify the underlying http headers with the given clone
    return this.clone();
  }

  @Override
  public int size() {
    return this.httpHeaders.size();
  }

  @Override
  public boolean contains(@NonNull String headerName) {
    return this.httpHeaders.contains(headerName);
  }

  @Override
  public boolean contains(@NonNull String headerName, @NonNull String headerValue) {
    return this.httpHeaders.contains(headerName, headerValue);
  }

  @Override
  public @Nullable String firstValue(@NonNull String headerName) {
    var headerValue = this.httpHeaders.get(headerName);
    return headerValue == null ? null : headerValue.toString();
  }

  @Override
  public @UnknownNullability String firstValue(@NonNull String headerName, @Nullable String defaultValue) {
    var headerValue = this.httpHeaders.get(headerName, defaultValue);
    return headerValue == null ? null : headerValue.toString();
  }

  @Override
  public @NonNull Collection<String> values(@NonNull String headerName) {
    var valuesIterator = this.httpHeaders.valuesIterator(headerName);
    return Lists.newArrayList(Iterators.transform(valuesIterator, CharSequence::toString));
  }

  @Override
  public @NonNull Set<String> names() {
    return this.httpHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toUnmodifiableSet());
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
    this.httpHeaders.add(headerName, headerValue);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String... headerValues) {
    this.httpHeaders.add(headerName, headerValues);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    this.httpHeaders.add(headerName, headerValues);
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
    this.httpHeaders.set(headerName, headerValue);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String... headerValues) {
    this.httpHeaders.set(headerName, headerValues);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    this.httpHeaders.set(headerName, headerValues);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String headerValue) {
    if (!this.httpHeaders.contains(headerName)) {
      return this.set(headerName, headerValue);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String... headerValues) {
    if (!this.httpHeaders.contains(headerName)) {
      return this.set(headerName, headerValues);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    if (!this.httpHeaders.contains(headerName)) {
      return this.set(headerName, headerValues);
    }
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap clear() {
    this.httpHeaders.clear();
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap remove(@NonNull String headerName) {
    this.httpHeaders.remove(headerName);
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap remove(@NonNull String headerName, @NonNull String headerValue) {
    this.httpHeaders.remove(headerName, headerValue);
    return this;
  }

  @Override
  public @NonNull Stream<Map.Entry<String, String>> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  @Override
  public @NonNull Collection<Map.Entry<String, String>> entries() {
    return Sets.newHashSet(this.iterator());
  }

  @Override
  public @NonNull Map<String, ? extends Collection<String>> asMap() {
    if (this.httpHeaders.isEmpty()) {
      return Map.of();
    } else {
      Map<String, Set<String>> resultMap = new HashMap<>(this.httpHeaders.size());
      for (var headerName : this.httpHeaders.names()) {
        var values = this.httpHeaders.valuesIterator(headerName);
        var toStringTransformIterator = Iterators.transform(values, CharSequence::toString);
        resultMap.put(headerName.toString(), Sets.newHashSet(toStringTransformIterator));
      }

      return resultMap;
    }
  }

  @Override
  public @NonNull Iterator<Map.Entry<String, String>> iterator() {
    return Iterators.transform(
      this.httpHeaders.iterator(),
      entry -> Map.entry(entry.getKey().toString(), entry.getValue().toString()));
  }

  @Override
  public @NonNull Spliterator<Map.Entry<String, String>> spliterator() {
    return Spliterators.spliterator(this.iterator(), this.size(), Spliterator.SIZED);
  }
}
