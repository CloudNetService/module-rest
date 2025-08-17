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

import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.handler.stream.ChunkedInput;
import java.io.InputStream;
import java.io.PushbackInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Temporary netty bug workaround - should not be used if not strictly necessary.
 */
@Deprecated // only to indicate that it shouldn't be used too much
final class NettyChunkedStream implements ChunkedInput<Buffer> {

  private static final int CHUNK_SIZE = 8192;

  private final PushbackInputStream in;

  private long offset;
  private boolean closed;
  private byte[] cachedArray;

  /**
   * Creates a new instance that fetches data from the specified stream.
   *
   * @param in the input stream that should be transferred.
   * @throws NullPointerException if the given input stream is null.
   */
  public NettyChunkedStream(@NonNull InputStream in) {
    this.in = in instanceof PushbackInputStream pbis ? pbis : new PushbackInputStream(in);
  }

  /**
   * Gets or create a cached heap array with a minimum length of the given size.
   *
   * @param minSize the minimum size that the cached array must have.
   * @return a cached heap array with the given minimum size.
   */
  private byte[] getOrCreateHeapBufferArray(int minSize) {
    if (this.cachedArray == null || this.cachedArray.length < minSize) {
      this.cachedArray = new byte[CHUNK_SIZE];
    }

    return this.cachedArray;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Buffer readChunk(@NonNull BufferAllocator allocator) throws Exception {
    if (this.isEndOfInput()) {
      return null;
    }

    var availableBytes = this.in.available();
    var chunkSize = availableBytes > 0 ? Math.min(CHUNK_SIZE, availableBytes) : CHUNK_SIZE;

    var release = true;
    var buffer = allocator.allocate(chunkSize);

    try {
      int written;
      try (var iter = buffer.forEachComponent()) {
        var comp = iter.firstWritable();
        if (comp.hasWritableArray()) {
          written = this.in.read(comp.writableArray(), comp.writableArrayOffset(), comp.writableArrayLength());
          if (written > 0) {
            comp.skipWritableBytes(written);
          }
        } else {
          var size = Math.min(comp.writableBytes(), chunkSize);
          var heapBuffer = this.getOrCreateHeapBufferArray(size);
          written = this.in.read(heapBuffer, 0, size);
          if (written > 0) {
            buffer.writeBytes(heapBuffer, 0, written);
          }
        }
      }

      if (written < 0) {
        return null;
      }

      this.offset += written;
      release = false;
      return buffer;
    } finally {
      if (release) {
        buffer.close();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEndOfInput() throws Exception {
    if (this.closed) {
      return true;
    }

    if (this.in.available() > 0) {
      return false;
    }

    int b = this.in.read();
    if (b < 0) {
      return true;
    } else {
      this.in.unread(b);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws Exception {
    this.closed = true;
    this.in.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long length() {
    return -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long progress() {
    return this.offset;
  }
}
