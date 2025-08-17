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

import com.google.common.base.MoreObjects;
import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpCookie;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.HttpVersion;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.handler.codec.http.DefaultFullHttpResponse;
import io.netty5.handler.codec.http.FullHttpResponse;
import io.netty5.handler.codec.http.HttpRequest;
import io.netty5.handler.codec.http.HttpResponseStatus;
import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import io.netty5.handler.codec.http.headers.HttpSetCookie;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of a http response.
 *
 * @since 1.0
 */
final class NettyHttpServerResponse extends NettyHttpMessage implements HttpResponse {

  final FullHttpResponse httpResponse;
  private final HttpHeaderMap httpHeaderMap;
  private final NettyHttpServerContext context;

  private InputStream responseInputStream;

  /**
   * Constructs a new netty http response instance.
   *
   * @param context     the context in which the request (and this response to the request) is handled.
   * @param httpRequest the original unwrapped request sent to the server.
   * @throws NullPointerException if either the context or request is null.
   */
  public NettyHttpServerResponse(@NonNull NettyHttpServerContext context, @NonNull HttpRequest httpRequest) {
    this.context = context;
    this.httpResponse = new DefaultFullHttpResponse(
      httpRequest.protocolVersion(),
      HttpResponseStatus.NOT_FOUND,
      DefaultBufferAllocators.offHeapAllocator().allocate(0));
    this.httpHeaderMap = new NettyHttpHeaderMap(this.httpResponse.headers());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponseCode status() {
    return HttpResponseCode.fromNumeric(this.httpResponse.status().code());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse status(@NonNull HttpResponseCode code) {
    this.httpResponse.setStatus(HttpResponseStatus.valueOf(code.code()));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext context() {
    return this.context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpHeaderMap headers() {
    return this.httpHeaderMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpVersion version() {
    return super.versionFromNetty(this.httpResponse.protocolVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse version(@NonNull HttpVersion version) {
    this.httpResponse.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] body() {
    var payload = this.httpResponse.payload();

    // initialize the body
    var length = payload.readableBytes();
    var body = new byte[length];

    // copy out the bytes of the buffer
    payload.copyInto(payload.readerOffset(), body, 0, length);
    return body;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse body(byte[] byteArray) {
    this.httpResponse.payload()
      .resetOffsets()
      .fill((byte) 0)
      .writeBytes(byteArray);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse body(@NonNull String text) {
    return this.body(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream bodyStream() {
    return this.responseInputStream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse body(InputStream body) {
    if (this.responseInputStream != null) {
      try {
        this.responseInputStream.close();
      } catch (IOException ignored) {
      }
    }

    this.responseInputStream = body;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasBody() {
    return this.httpResponse.payload().readableBytes() > 0 || this.responseInputStream != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpCookie cookie(@NonNull String name) {
    var cookie = this.httpResponse.headers().getSetCookie(name);
    return cookie == null ? null : this.convertFromNettySetCookie(cookie);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<HttpCookie> cookies() {
    List<HttpCookie> cookies = new ArrayList<>();
    this.httpResponse.headers().getSetCookies().forEach(cookie -> {
      var convertedCookie = this.convertFromNettySetCookie(cookie);
      cookies.add(convertedCookie);
    });
    return cookies;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasCookie(@NonNull String name) {
    return this.httpResponse.headers().getSetCookie(name) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse cookies(@NonNull Collection<HttpCookie> cookies) {
    this.httpResponse.headers().remove(HttpHeaders.SET_COOKIE);
    cookies.forEach(this::addCookie);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse addCookie(@NonNull HttpCookie httpCookie) {
    var convertedCookie = this.convertToNettySetCookie(httpCookie);
    this.httpResponse.headers().addSetCookie(convertedCookie);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse removeCookie(@NonNull String name) {
    this.httpResponse.headers().removeSetCookies(name);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse clearCookies() {
    this.httpResponse.headers().remove(HttpHeaders.SET_COOKIE);
    return this;
  }

  /**
   * Converts the netty set-cookie value to a {@link HttpCookie}.
   *
   * @param cookie the cookie to convert.
   * @return the converted cookie.
   * @throws NullPointerException if the given cookie is null.
   */
  private @NonNull HttpCookie convertFromNettySetCookie(@NonNull HttpSetCookie cookie) {
    return new HttpCookie(
      cookie.name().toString(),
      cookie.value().toString(),
      Objects.toString(cookie.domain(), null),
      Objects.toString(cookie.path(), null),
      cookie.isHttpOnly(),
      cookie.isSecure(),
      cookie.isWrapped(),
      MoreObjects.firstNonNull(cookie.maxAge(), Long.MAX_VALUE));
  }

  /**
   * Converts the given {@link HttpCookie} to a netty set-cookie value.
   *
   * @param cookie the cookie to convert.
   * @return the converted cookie.
   * @throws NullPointerException if the given cookie is null.
   */
  private @NonNull HttpSetCookie convertToNettySetCookie(@NonNull HttpCookie cookie) {
    return new DefaultHttpSetCookie(
      cookie.name(),
      cookie.value(),
      cookie.path(),
      cookie.domain(),
      null,
      cookie.maxAge() == Long.MAX_VALUE ? null : cookie.maxAge(),
      null,
      cookie.wrap(),
      cookie.secure(),
      cookie.httpOnly());
  }
}
