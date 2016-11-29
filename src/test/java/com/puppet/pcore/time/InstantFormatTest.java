package com.puppet.pcore.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstantFormatTest {
	@Test
	public void t1() {
		assertEquals(Instant.parse("2012-10-11T00:00:00Z"), parse("12-10-11", "%y-%m-%d"));
	}

	@Test
	public void t2() {
		assertEquals(Instant.parse("2012-10-11T00:00:00Z"), parse("12-OCTOBER-11", "%y-%^B-%d"));
	}

	@Test
	public void f1() {
		assertEquals("12-OCTOBER-11", format(Instant.parse("2012-10-11T00:00:00Z"), "%y-%^B-%d", "+0200"));
	}

	@Test
	public void f2() {
		assertEquals("12-OCTOBER-11 02:23 +0200", format(Instant.parse("2012-10-11T00:23:00Z"), "%y-%^B-%d %H:%M %z", "+0200"));
	}

	@Test
	public void f3() {
		assertEquals("Thu Oct 11 02:23:00 2012", format(Instant.parse("2012-10-11T00:23:00Z"), "%c", "+0200"));
	}

	@Test
	public void f4() {
		assertEquals("10/11/12 02:23:00 AM", format(Instant.parse("2012-10-11T00:23:00Z"), "%D %r", "+0200"));
	}

	@Test
	public void f5() {
		assertEquals("10/11/12 02:23:00 am", format(Instant.parse("2012-10-11T00:23:00Z"), "%D %I:%M:%S %P", "+0200"));
	}

	@Test
	public void f6() {
		assertEquals("  12-010-11", format(Instant.parse("2012-10-11T00:23:00Z"), "%_4y-%3m-%d", "+0000"));
	}

	private Instant parse(String instant, String pattern) {
		return new InstantFormat().parse(instant, pattern);
	}

	private String format(Instant instant, String pattern, String zoneId) {
		return new InstantFormat().format(instant, pattern, ZoneId.of(zoneId));
	}
}
