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

package eu.cloudnetservice.ext.rest.api.util;

import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility to parse/format http-dates as defined in <a
 * href="https://datatracker.ietf.org/doc/html/rfc9110#section-5.6.7">RFC 9110 Section 5.6.7</a>.
 */
public final class HttpDateUtil {

  private static final ZoneId GMT = ZoneId.of("GMT");

  /**
   * Date format as defined as the preferred format in the above RFC, used for outbound header formatting.
   */
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    .withZone(GMT);

  /**
   * Date formats used for parsing incoming date header fields. Formats of all 3 are defined in the above RFC.
   */
  private static final List<DateTimeFormatter> DATE_PARSERS = List.of(
    DateTimeFormatter.RFC_1123_DATE_TIME, // IMF-fixdate
    DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US), // RFC 850
    DateTimeFormatter.ofPattern("EEE MMM [ d][dd] HH:mm:ss yyyy", Locale.US).withZone(GMT) // ANSI C's asctime()
  );

  private HttpDateUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Parses an http header to a date-time if possible using one the formats defined in RFC 9110.
   *
   * @param headers    the header map to obtain the header value from.
   * @param headerName the name of the header to convert into a date-time.
   * @return the parsed date-time from the given header or {@code null} if parsing was not possible.
   * @throws NullPointerException if the given header map or header name is null.
   */
  public static @Nullable ZonedDateTime parseHttpDate(@NonNull HttpHeaderMap headers, @NonNull String headerName) {
    var headerValue = headers.firstValue(headerName);
    return headerValue == null ? null : parseHttpDate(headerValue);
  }

  /**
   * Parses the given string to a date-time if possible using one the formats defined in RFC 9110.
   *
   * @param dateValue the date value to parse into a date-time.
   * @return the parsed date-time from the given value or {@code null} if parsing was not possible.
   * @throws NullPointerException if the given value is null.
   */
  public static @Nullable ZonedDateTime parseHttpDate(@NonNull String dateValue) {
    for (var dateParser : DATE_PARSERS) {
      try {
        return ZonedDateTime.parse(dateValue, dateParser);
      } catch (DateTimeParseException ignored) {
      }
    }

    return null;
  }

  /**
   * Formats the given instant in the preferred http date format as defined in RFC 9110.
   *
   * @param instant the instant to format.
   * @return the instant, formatted as an RFC 9110 compliant string.
   * @throws NullPointerException if the given instant is null.
   */
  public static @NonNull String formatAsHttpDate(@NonNull Instant instant) {
    return formatAsHttpDate(ZonedDateTime.ofInstant(instant, GMT));
  }

  /**
   * Formats the given date-time in the preferred http date format as defined in RFC 9110.
   *
   * @param zdt the date-time to format.
   * @return the date-time, formatted as an RFC 9110 compliant string.
   * @throws NullPointerException if the given date-time is null.
   */
  public static @NonNull String formatAsHttpDate(@NonNull ZonedDateTime zdt) {
    return DATE_FORMATTER.format(zdt);
  }
}
