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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HttpDateUtilTest {

  static Stream<Arguments> httpDateOutputSource() {
    return Stream.of(
      Arguments.of(
        ZonedDateTime.of(1994, 11, 6, 8, 49, 37, 0, ZoneOffset.UTC),
        "Sun, 06 Nov 1994 08:49:37 GMT"),
      Arguments.of(
        ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        "Sat, 01 Jan 2000 00:00:00 GMT"),
      Arguments.of(
        ZonedDateTime.of(2015, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC),
        "Thu, 31 Dec 2015 23:59:59 GMT"),
      Arguments.of(
        ZonedDateTime.of(2020, 2, 29, 12, 30, 45, 0, ZoneOffset.UTC),
        "Sat, 29 Feb 2020 12:30:45 GMT"),
      Arguments.of(
        ZonedDateTime.of(2025, 9, 6, 14, 15, 0, 0, ZoneOffset.UTC),
        "Sat, 06 Sep 2025 14:15:00 GMT"));
  }

  static Stream<Arguments> httpDateVariantsSource() {
    return Stream.of(
      Arguments.of(
        List.of(
          "Sun, 06 Nov 1994 08:49:37 GMT",
          "Sun Nov  6 08:49:37 1994"),
        LocalDateTime.of(1994, 11, 6, 8, 49, 37)),
      Arguments.of(
        List.of(
          "Sat, 02 Jan 2010 00:00:00 GMT",
          "Saturday, 02-Jan-10 00:00:00 GMT",
          "Sat Jan  2 00:00:00 2010"),
        LocalDateTime.of(2010, 1, 2, 0, 0, 0)),
      Arguments.of(
        List.of(
          "Fri, 31 Dec 2021 23:59:59 GMT",
          "Friday, 31-Dec-21 23:59:59 GMT",
          "Fri Dec 31 23:59:59 2021"),
        LocalDateTime.of(2021, 12, 31, 23, 59, 59)),
      Arguments.of(
        List.of(
          "Tue, 19 Jan 2038 03:14:07 GMT",
          "Tuesday, 19-Jan-38 03:14:07 GMT",
          "Tue Jan 19 03:14:07 2038"),
        LocalDateTime.of(2038, 1, 19, 3, 14, 7)),
      Arguments.of(
        List.of(
          "Thu, 29 Feb 2024 12:30:15 GMT",
          "Thursday, 29-Feb-24 12:30:15 GMT",
          "Thu Feb 29 12:30:15 2024"),
        LocalDateTime.of(2024, 2, 29, 12, 30, 15)));
  }

  @Test
  void testFormatFromInstantRfc1123() {
    var epoch = Instant.ofEpochSecond(0);
    var formatted = HttpDateUtil.formatAsHttpDate(epoch);
    Assertions.assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", formatted);
  }

  @Test
  void testFormatFromZdtRfc1123() {
    var zi = ZoneId.of("Europe/Berlin");
    var zdt = ZonedDateTime.of(2025, 9, 6, 14, 34, 56, 0, zi);
    var formatted = HttpDateUtil.formatAsHttpDate(zdt);
    Assertions.assertEquals("Sat, 06 Sep 2025 12:34:56 GMT", formatted);
  }

  @ParameterizedTest
  @MethodSource("httpDateVariantsSource")
  void testParseHttpDate(List<String> variants, LocalDateTime expected) {
    for (var variant : variants) {
      var parsed = HttpDateUtil.parseHttpDate(variant);
      Assertions.assertNotNull(parsed, variant);
      Assertions.assertEquals(expected, parsed.toLocalDateTime());
    }
  }

  @ParameterizedTest
  @MethodSource("httpDateOutputSource")
  void testHttpDateFormatting(ZonedDateTime input, String expected) {
    Assertions.assertEquals(expected, HttpDateUtil.formatAsHttpDate(input));
  }

  @Test
  void testUnparsableDateValueReturnsNull() {
    Assertions.assertNull(HttpDateUtil.parseHttpDate("0"));
    Assertions.assertNull(HttpDateUtil.parseHttpDate("1757168445"));
    Assertions.assertNull(HttpDateUtil.parseHttpDate("not a date at all"));
    Assertions.assertNull(HttpDateUtil.parseHttpDate("2025-09-06T12:34:56Z"));
  }

  @Test
  void testParseDateFromHeader() {
    var headerMap = HttpHeaderMap.newHeaderMap();
    headerMap.set("Date1", "Sat Jan  2 00:00:00 2010");
    headerMap.set("Date2", "Sat, 02 Jan 2010 00:00:00 GMT");
    headerMap.set("Date3", "Saturday, 02-Jan-10 00:00:00 GMT");

    var expected = LocalDateTime.of(2010, 1, 2, 0, 0, 0);
    var date1Parsed = HttpDateUtil.parseHttpDate(headerMap, "Date1");
    Assertions.assertNotNull(date1Parsed);
    Assertions.assertEquals(expected, date1Parsed.toLocalDateTime());

    var date2Parsed = HttpDateUtil.parseHttpDate(headerMap, "Date2");
    Assertions.assertNotNull(date2Parsed);
    Assertions.assertEquals(expected, date2Parsed.toLocalDateTime());

    var date3Parsed = HttpDateUtil.parseHttpDate(headerMap, "Date3");
    Assertions.assertNotNull(date3Parsed);
    Assertions.assertEquals(expected, date3Parsed.toLocalDateTime());
  }

  @Test
  void testUsesFirstHeaderValue() {
    var headerMap = HttpHeaderMap.newHeaderMap();
    headerMap.add("Date1", "Sat, 02 Jan 2010 00:00:00 GMT");
    headerMap.add("Date1", "Fri, 31 Dec 2021 23:59:59 GMT");

    var expected = LocalDateTime.of(2010, 1, 2, 0, 0, 0);
    var date1Parsed = HttpDateUtil.parseHttpDate(headerMap, "Date1");
    Assertions.assertNotNull(date1Parsed);
    Assertions.assertEquals(expected, date1Parsed.toLocalDateTime());
  }

  @Test
  void testMissingHeaderResultsInNull() {
    var headerMap = HttpHeaderMap.newHeaderMap();
    headerMap.add("Date1", "Sat, 02 Jan 2010 00:00:00 GMT");
    Assertions.assertNull(HttpDateUtil.parseHttpDate(headerMap, "Date2"));
  }
}
