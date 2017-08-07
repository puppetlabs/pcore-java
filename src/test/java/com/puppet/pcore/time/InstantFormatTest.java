package com.puppet.pcore.time;

import com.puppet.pcore.impl.Helpers;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static com.puppet.pcore.TestHelper.assertMatches;
import static com.puppet.pcore.TestHelper.dynamicMapTest;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Helpers.entry;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("InstantFormat")
public class InstantFormatTest {
	@Nested
	@DisplayName("whe used for parsing")
	class Parsing {
		private Instant parse(String instant) {
			return new InstantFormat().parse(instant);
		}

		private Instant parse(String instant, String pattern) {
			return new InstantFormat().parse(instant, pattern);
		}

		@SuppressWarnings("unchecked")
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndArg() {
			return dynamicMapTest(
					asMap(
							entry("%FT%T.%N", entry("2012-10-11T11:12:13.003211", "2012-10-11T11:12:13.003211Z")),
							entry("%y-%m-%d", entry("12-10-11", "2012-10-11T00:00:00Z")),
							entry("%y-%^B-%d", entry("12-OCTOBER-11", "2012-10-11T00:00:00Z")),
							entry("%y-%m-%d %H:%M:%S", entry("12-10-11 13:15:14", "2012-10-11T13:15:14Z"))
					),
					(fmt, e) -> format("the format '%s' parses '%s' into '%s'", fmt, e.key, e.value),
					(fmt, e) -> assertEquals(Instant.parse(e.value), parse(e.key, fmt))
			);
		}

		@SuppressWarnings("unchecked")
		@TestFactory
		@DisplayName("the string")
		Iterable<DynamicTest> parsedUsingDefault() {
			return dynamicMapTest(
					asMap(
							entry("2012-10-11T13:15:14.003", "2012-10-11T13:15:14.003Z"),
							entry("2012-10-11T13:15:14", "2012-10-11T13:15:14Z"),
							entry("2012-10-11 13:15:14", "2012-10-11T13:15:14Z"),
							entry("2012-10-11", "2012-10-11T00:00:00Z")
					),
					(fmt, result) -> format("'%s' is parsed int '%s' using a default format", fmt, result),
					(fmt, result) -> assertEquals(Instant.parse(result), parse(fmt))
			);
		}

		@Test
		@DisplayName("fails on bad format specifier")
		void failsUnrecognized() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("12-10-11", "%y-%m-%q"));
			assertEquals("Bad format specifier '%q' in '%y-%m-%q' at position 6", ex.getMessage());
		}

		@Test
		@DisplayName("fails on when colon flag is used with other format than z")
		void failsColonNotz() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("12-10-11", "%y-%m-%:d"));
			assertEquals("Bad format specifier '%:d' in '%y-%m-%:d' at position 6", ex.getMessage());
		}

		@Test
		@DisplayName("fails on more than two colon flags")
		void moreThanTwoColons() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("2012-10-11 14:12:13 +03:00:00", "%F %T %:::z"));
			assertEquals("Bad format specifier '%:::' in '%F %T %:::z' at position 6", ex.getMessage());
		}

		@Test
		@DisplayName("fails when no default format is able to parse")
		void failNoMatchToDefault() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("12/10/11"));
			assertMatches("Unable to parse '12/10/11' using any of the default formats", ex.getMessage());
		}

		@Test
		@DisplayName("and neither time or date is extracted")
		void noTimeNorDate() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("%and%", "%%and%%"));
			assertMatches("Unable to extract time and/or date from string '%and%'", ex.getMessage());
		}

		@Test
		@DisplayName("fails on incomplete format")
		void untermintatedFormatSpec() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("2012-10-11 14:12:13", "%F %"));
			assertMatches("Bad format specifier '%' in '%F %' at position 3", ex.getMessage());
		}

		@Test
		@DisplayName("fails on incomplete format with flags")
		void untermintatedFormatSpecWithFlags() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("2012-10-11 14:12:13", "%F %0"));
			assertMatches("Bad format specifier '%0' in '%F %0' at position 3", ex.getMessage());
		}

		@Test
		@DisplayName("fails on incomplete format with width")
		void untermintatedFormatSpecWithWidth() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> parse("2012-10-11 14:12:13", "%F %10"));
			assertMatches("Bad format specifier '%10' in '%F %10' at position 3", ex.getMessage());
		}
	}


	@Nested
	@DisplayName("when used for formatting")
	class Formatting {
		private String iformat(Instant instant, String pattern, String zoneId) {
			return new InstantFormat().format(instant, pattern, ZoneId.of(zoneId));
		}

		@SuppressWarnings("unchecked")
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndArg() {
			Map<String,Helpers.MapEntry<String,Helpers.MapEntry<String, String>>> tests = new LinkedHashMap<>();
			tests.putAll(
					Helpers.asMap(
							entry("%y-%m-%d", entry("12-10-11", entry("2012-10-11T00:00:00Z", "+0200"))),
							entry("%y-%B-%d", entry("12-October-11", entry("2012-10-11T00:00:00Z", "+0200"))),
							entry("%y-%^B-%d", entry("12-OCTOBER-11", entry("2012-10-11T00:00:00Z", "+0200"))),
							entry("%y-%h-%d", entry("12-Oct-11", entry("2012-10-11T00:00:00Z", "+0200"))),
							entry("%y-%^h-%d", entry("12-OCT-11", entry("2012-10-11T00:00:00Z", "+0200"))),
							entry("%y-%^B-%d %H:%M %z", entry("12-OCTOBER-11 02:23 +0200", entry("2012-10-11T00:23:00Z", "+0200")))));

			tests.putAll(
					Helpers.asMap(
							entry("%y-%j", entry("12-285", entry("2012-10-11T00:00:00Z", "+0200"))),
							entry("%c", entry("Thu Oct 11 02:23:00 2012", entry("2012-10-11T00:23:00Z", "+0200"))),
							entry("%D %r", entry("10/11/12 02:23:00 AM", entry("2012-10-11T00:23:00Z", "+0200"))),
							entry("%D %I:%M:%S %P", entry("10/11/12 02:23:00 am", entry("2012-10-11T00:23:00Z", "+0200"))),
							entry("%_12y-%3m-%d", entry("          12-010-11", entry("2012-10-11T00:23:00Z", "+0000"))),
							entry("%04y-%3m-%d", entry("0012-010-11", entry("2012-10-11T00:23:00Z", "+0000")))));

			tests.putAll(
					Helpers.asMap(
							entry("%-4y-%3m-%d", entry("12-010-11", entry("2012-10-11T00:23:00Z", "+0000"))),
							entry("%FT%T.%N", entry("2012-10-11T11:12:13.003211", entry("2012-10-11T11:12:13.003211Z", "+0000"))),
							entry("%F %T %z", entry("2012-10-11 14:12:13 +0300", entry("2012-10-11T11:12:13Z", "+0300"))),
							entry("%F %T %:z", entry("2012-10-11 14:12:13 +03:00", entry("2012-10-11T11:12:13Z", "+0300"))),
							entry("%F %T %::z", entry("2012-10-11 14:12:13 +03:00:00", entry("2012-10-11T11:12:13Z", "+0300"))),
							entry("%A", entry("Thursday", entry("2012-10-11T13:02:03.123Z", "+0000")))));

			tests.putAll(
					Helpers.asMap(
							entry("%k", entry(" 1", entry("2012-10-11T01:02:03Z", "+0000"))),
							entry("%l", entry(" 1", entry("2012-10-11T13:02:03Z", "+0000"))),
							entry("%l:%M:%S.%L", entry(" 1:02:03.123", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%^A", entry("THURSDAY", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%a", entry("Thu", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%^a", entry("THU", entry("2012-10-11T13:02:03.123Z", "+0000")))));

			tests.putAll(
					Helpers.asMap(
							entry("%u", entry("4", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%w", entry("5", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%G", entry("2012", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%g", entry("12", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%V", entry("41", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%U", entry("41", entry("2012-10-11T13:02:03.123Z", "+0000")))));

			tests.putAll(
					Helpers.asMap(
							entry("%W", entry("41", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%s", entry("1349960523", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%F%n%T", entry("2012-10-11\n13:02:03", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%F%t%T", entry("2012-10-11\t13:02:03", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%v", entry("11-OCT-2012", entry("2012-10-11T13:02:03.123Z", "+0000"))),
							entry("%R", entry("13:02", entry("2012-10-11T13:02:03.123Z", "+0000")))));

			return dynamicMapTest(tests,
					(fmt, e) -> format("the format '%s' formats '%s' into '%s' using zone '%s'", fmt, e.value.key, e.key, e.value.value),
					(fmt, e) -> assertEquals(e.key, iformat(Instant.parse(e.value.key), fmt, e.value.value)));
		}
	}
}
