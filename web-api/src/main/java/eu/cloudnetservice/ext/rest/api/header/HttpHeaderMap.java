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

package eu.cloudnetservice.ext.rest.api.header;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A http header map containing header names (which act as keys here) and their respective header values. Because this
 * is a multimap, a list of header values can be stored for each header name (key).
 * <p>
 * The unmodifiable version obtained using {@link #unmodifiableClone()} does not allow any modifications and any call
 * will trigger an {@link UnsupportedOperationException}.
 *
 * @since 1.0
 */
public interface HttpHeaderMap extends Iterable<Map.Entry<String, String>>, Cloneable {

  /**
   * Constructs a new empty http header map with an initial size of 16.
   *
   * @return a new empty http header map.
   */
  static @NonNull HttpHeaderMap newHeaderMap() {
    return newHeaderMap(16);
  }

  /**
   * Constructs a new empty http header map with the given initial size.
   *
   * @param sizeHint the initial key amount.
   * @return a new empty http header map.
   */
  static @NonNull HttpHeaderMap newHeaderMap(int sizeHint) {
    return new HashHttpHeaderMap(sizeHint);
  }

  /**
   * Creates a mutable clone of this http header map.
   *
   * @return a mutable clone of this http header map.
   */
  @NonNull HttpHeaderMap clone();

  /**
   * Creates an unmodifiable clone of this http header map. Modifications to the created map result in an
   * {@link UnsupportedOperationException}.
   *
   * @return an unmodifiable clone of this http header map.
   */
  @NonNull HttpHeaderMap unmodifiableClone();

  /**
   * Gets how many key-value header pairs are stored in this map.
   *
   * @return how many key-value header pairs are stored in this map.
   */
  int size();

  /**
   * Gets whether this http header map contains a key-value mapping for the given header name (key).
   *
   * @param headerName the key used to store the header value with.
   * @return true if this map contains at least one mapping, false otherwise.
   * @throws NullPointerException if the given header name is null.
   */
  boolean contains(@NonNull String headerName);

  /**
   * Gets whether this http header map contains a key-value mapping with both the given key and the given value.
   *
   * @param headerName  the key used to store the header value with.
   * @param headerValue the header value that was stored.
   * @return true if this map contains at least one mapping, false otherwise.
   * @throws NullPointerException if the given header name or value is null.
   */
  boolean contains(@NonNull String headerName, @NonNull String headerValue);

  /**
   * Gets the first value associated with the given header name or null if no value is associated.
   *
   * @param headerName the header name the value is associated with.
   * @return the first value associated with the given header name or null if no value is associated.
   * @throws NullPointerException if the given header name is null.
   */
  @Nullable String firstValue(@NonNull String headerName);

  /**
   * Gets the first value associated with the given header name and returns the default value is no value is
   * associated.
   *
   * @param headerName   the header name the value is associated with.
   * @param defaultValue the default value to return if no value is associated.
   * @return the first value associated with the given header name and returns the default value is no value is
   * associated.
   * @throws NullPointerException if the given header name is null.
   */
  @UnknownNullability String firstValue(@NonNull String headerName, @Nullable String defaultValue);

  /**
   * Gets all values associated with the given header name.
   *
   * @param headerName the header name to get the associated values for.
   * @return all values associated with the given header name.
   * @throws NullPointerException if the given header name is null.
   */
  @NonNull Collection<String> values(@NonNull String headerName);

  /**
   * Gets all header names (keys) that are associated with at least one value in this map.
   *
   * @return all header names (keys) that are associated with at least one value in this map.
   */
  @NonNull Set<String> names();

  /**
   * Adds all key-value pairs from the given http header map into this header map without replacing already existing
   * ones.
   *
   * @param headerMap the header map to add into this one.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header map is null.
   */
  @NonNull HttpHeaderMap add(@NonNull HttpHeaderMap headerMap);

  /**
   * Adds all key-value pairs from the given header map into this header map without replacing already existing ones.
   *
   * @param headers the map to add into this http header map.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given map is null.
   */
  @NonNull HttpHeaderMap add(@NonNull Map<String, ? extends Iterable<String>> headers);

  /**
   * Adds a single key-value pair to this header map without replacing already existing ones.
   *
   * @param headerName  the key (header name) of the about to be added key-value pair.
   * @param headerValue the header value of the about to be added key-value pair.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name or value is null.
   */
  @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String headerValue);

  /**
   * Adds all given header values with the given header name (key) into this header map without replacing already
   * existing ones.
   *
   * @param headerName   the key (header name) of the about to be added key-value pair.
   * @param headerValues the header values to associate with the given key.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name, the array or any of the arrays values is null.
   */
  @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String... headerValues);

  /**
   * Adds all given header values with the given header name (key) into this header map without replacing already
   * existing ones.
   *
   * @param headerName   the key (header name) of the about to be added key-value pair.
   * @param headerValues the header values to associate with the given key.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name, the iterable or any of the iterables values is null.
   */
  @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull Iterable<String> headerValues);

  /**
   * Replaces all header key-value pairs in this map that are present in the given header map.
   *
   * @param headerMap the header map to take the key-value pairs from.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header map is null.
   */
  @NonNull HttpHeaderMap set(@NonNull HttpHeaderMap headerMap);

  /**
   * Replaces all header key-value pairs in this map that are present in the given map.
   *
   * @param headers the map to take the key-value pairs from.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given map is null.
   */
  @NonNull HttpHeaderMap set(@NonNull Map<String, ? extends Iterable<String>> headers);

  /**
   * Replaces the all values associated with the given header name (key) with the given header value.
   *
   * @param headerName  the key the header value is associated with.
   * @param headerValue the header value to replace the old values with.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name or value is null.
   */
  @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String headerValue);

  /**
   * Replaces all values associated with the given header name (key) with the given header values.
   *
   * @param headerName   the key the header values are associated with.
   * @param headerValues the header values to replace the old values with.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name, the array or any of the arrays values is null.
   */
  @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String... headerValues);

  /**
   * Replaces all values associated with the given header name (key) with the given header values.
   *
   * @param headerName   the key the header values are associated with.
   * @param headerValues the header values to replace the old values with.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name, the iterable or any of the iterables values is null.
   */
  @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull Iterable<String> headerValues);

  /**
   * Sets the given key-value pair if there is no value associated with the given header name (key).
   *
   * @param headerName  the key the header value is associated with.
   * @param headerValue the header value to set.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name or value is null.
   */
  @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String headerValue);

  /**
   * Sets the given key-value pair if there is no value associated with the given header name (key).
   *
   * @param headerName   the key the header values are associated with.
   * @param headerValues the header values to associate with the given key.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name, the array or any of the arrays values is null.
   */
  @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String... headerValues);

  /**
   * Sets the given key-value pair if there is no value associated with the given header name (key).
   *
   * @param headerName   the key the header values are associated with.
   * @param headerValues the header values to associate with the given key.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name, the iterable or any of the iterables values is null.
   */
  @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull Iterable<String> headerValues);

  /**
   * Clears this header map - no key-pairs are left after calling.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  @NonNull HttpHeaderMap clear();

  /**
   * Removes all values associated with the given header name (key).
   *
   * @param headerName the header name the values are associated with.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name is null.
   */
  @NonNull HttpHeaderMap remove(@NonNull String headerName);

  /**
   * Removes the given key-value pair from this map without removing other header values that might be associated with
   * the given header name.
   *
   * @param headerName  the header name the given value is associated with.
   * @param headerValue the header value to remove.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given header name or value is null.
   */
  @NonNull HttpHeaderMap remove(@NonNull String headerName, @NonNull String headerValue);

  /**
   * Streams all key-value pairs in this map.
   *
   * @return all key-value pairs in this map.
   */
  @NonNull Stream<Map.Entry<String, String>> stream();

  /**
   * Gets all key-value pairs in this map.
   *
   * @return all key-value pairs in this map.
   */
  @NonNull Collection<Map.Entry<String, String>> entries();

  /**
   * Gets this http header multimap as map.
   *
   * @return this http header multimap as map.
   */
  @NonNull Map<String, ? extends Collection<String>> asMap();
}
