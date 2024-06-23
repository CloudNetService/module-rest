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

package eu.cloudnetservice.ext.modules.rest;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import lombok.NonNull;

public final class UUIDv7 {

  private static final Random RANDOM = new SecureRandom();
  private static final VarHandle BYTE_ARRAY_LONG_VIEW_HANDLE =
    MethodHandles.byteArrayViewVarHandle(Long.TYPE.arrayType(), ByteOrder.BIG_ENDIAN);

  private UUIDv7() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull UUID generate(long timestamp) {
    // get the random data for the second uuid component
    var bytes = new byte[10];
    RANDOM.nextBytes(bytes);
    var randA = ((bytes[0] & 0xFF) << 8) + (bytes[1] & 0xFF);
    var randB = (long) BYTE_ARRAY_LONG_VIEW_HANDLE.get(bytes, 2);

    // encode the timestamp and random components into an uuid
    var rawMsb = (timestamp << 16) | randA;
    var msb = (rawMsb & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L; // set version to 7
    var lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;  // set variant to DCE 1.1
    return new UUID(msb, lsb);
  }

  public static @NonNull UUID fromString(@NonNull String uniqueId) {
    return UUID.fromString(uniqueId);
  }
}
