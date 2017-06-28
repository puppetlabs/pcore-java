/*
  Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
  <p>
  Contributors:
  Puppet Labs
 */
package com.puppet.pcore.semver.tests;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VersionRange.
 */
public class VersionRangeTest {

	@Test
	public void dashRange() {
		VersionRange range = VersionRange.create("1.2.0 - 1.3.0");
		assertTrue(range.includes(Version.create(1, 2, 0)));
		assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
		assertTrue(range.includes(Version.create(1, 3, 0)));
		assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
		assertFalse(range.includes(Version.create(1, 3, 1)));

		range = VersionRange.create("1.2.0 - 1.3.0-alpha");
		assertTrue(range.includes(Version.create(1, 2, 0)));
		assertTrue(range.includes(Version.create(1, 3, 0, "alpha")));
		assertFalse(range.includes(Version.create(1, 3, 0)));

		range = VersionRange.create("1.2.0-alpha - 1.3.0");
		assertTrue(range.includes(Version.create(1, 2, 0)));
		assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
		assertTrue(range.includes(Version.create(1, 3, 0)));
		assertTrue(range.includes(Version.create(1, 2, 0, "alpha")));
	}

	@Test
	public void greater() {
		try {
			VersionRange range = VersionRange.create(">1.2.0");
			assertEquals(VersionRange.greater(Version.create("1.2.0")), range);
			assertTrue(range.isExcludeBegin());

			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 2, 9, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 1)));
			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 2, 0)));

			range = VersionRange.create(">1.2.0--");
			assertTrue(range.includes(Version.create(1, 2, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 0)));
			assertFalse(range.includes(Version.create(1, 1, 9)));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqual() {
		try {
			VersionRange range = VersionRange.create(">=1.2.0");
			assertEquals(VersionRange.greaterOrEqual(Version.create("1.2.0")), range);
			assertFalse(range.isExcludeBegin());

			assertTrue(range.includes(Version.create(1, 2, 0)));
			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 2, 9, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 1)));
			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));

			range = VersionRange.create(">=1.2.0--");
			assertTrue(range.includes(Version.create(1, 2, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 0)));
			assertTrue(range.includes(Version.create(1, 2, 0, "-")));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void asRestrictiveAs() {
		VersionRange range1 = VersionRange.create(">=1.2.0 <1.3.0--");
		VersionRange range2 = VersionRange.create(">=1.2.1 <1.2.2--");
		assertTrue(range2.isAsRestrictiveAs(range1));
		assertFalse(range1.isAsRestrictiveAs(range2));
	}

	@Test
	public void createVersionArgs() {
		assertEquals(VersionRange.create(">=1.2.0 <1.3.0"), VersionRange.create(Version.create("1.2.0"), true, Version.create("1.3.0"), false));
		assertEquals(VersionRange.create(">1.2.0 <=1.3.0"), VersionRange.create(Version.create("1.2.0"), false, Version.create("1.3.0"), true));
	}

	@Test
	public void intersection() {
		try {
			VersionRange range1 = VersionRange.create(">1.2.0");
			VersionRange range2 = VersionRange.create("<1.2.0");
			assertNull(range1.intersection(range2));

			range1 = VersionRange.create(">1.2.0");
			range2 = VersionRange.create("<1.1.0");
			assertNull(range1.intersection(range2));

			range1 = VersionRange.create(">1.2.0");
			range2 = VersionRange.create("<=1.2.0");
			assertNull(range1.intersection(range2));

			range1 = VersionRange.create(">=1.2.0 <1.3.0");
			range2 = VersionRange.create(">=1.3.0 <1.4.0");
			assertNull(range1.intersection(range2));

			range1 = VersionRange.create(">=1.2.0 <1.3.0");
			range2 = VersionRange.create(">=1.2.0 <1.4.0");
			assertNotNull(range1.intersection(range2));

			range1 = VersionRange.create(">=1.2.0");
			range2 = VersionRange.create("<=1.2.0");
			assertNotNull(range1.intersection(range2));

			range1 = VersionRange.create("<1.2.0");
			range2 = VersionRange.create(">1.1.0");
			assertNotNull(range1.intersection(range2));

			range1 = VersionRange.create(">1.1.0");
			range2 = VersionRange.create("<1.2.0");
			assertNotNull(range1.intersection(range2));

			range1 = VersionRange.create("<1.1.0");
			range2 = VersionRange.create("<1.2.0");
			assertNotNull(range1.intersection(range2));

			range1 = VersionRange.create(">=1.2.0 <1.3.0--");
			range2 = VersionRange.create(">=1.3.0");
			assertNull(range1.intersection(range2));

			range1 = VersionRange.create(">=1.2.0 <1.3.0--");
			range2 = VersionRange.create(">=1.2.1 <1.2.2--");
			assertNotNull(range1.intersection(range2));

			range1 = VersionRange.create("<1.3.0-- >=1.2.0");
			range2 = VersionRange.create("<1.2.2-- >=1.2.1");
			assertNotNull(range1.intersection(range2));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void union() {
		assertEquals(">1.2.0 <=1.4.0", VersionRange.create(">1.2.0 <=1.3.0 || >1.3.0 <=1.4.0").toNormalizedString());
		assertEquals(">1.2.0 <=1.4.0", VersionRange.create(">1.3.0 <=1.4.0 || >1.2.0 <=1.3.0").toNormalizedString());
		assertEquals(">1.2.0 <=1.4.0", VersionRange.create(">1.2.0 <=1.4.0 || >1.2.0 <=1.3.0").toNormalizedString());
		assertEquals(">1.2.0 <=1.4.0", VersionRange.create(">1.2.0 <=1.4.0 || >1.3.0 <=1.4.0").toNormalizedString());
		assertEquals(">1.2.0 <1.4.0", VersionRange.create(">1.2.0 <1.4.0 || >1.3.0 <1.4.0").toNormalizedString());
		assertEquals(">=1.2.0 <=1.4.0", VersionRange.create(">=1.2.0 <=1.4.0 || >1.3.0 <=1.4.0").toNormalizedString());
		assertEquals(">=1.2.0 <1.4.0", VersionRange.create(">=1.2.0 <1.4.0 || >1.3.0 <1.4.0").toNormalizedString());
		assertEquals(">=1.2.0 <1.4.0", VersionRange.create(">=1.2.0 <1.4.0 || >=1.2.0 <1.3.0").toNormalizedString());
		assertEquals(">=1.2.0 <1.4.0", VersionRange.create(">=1.2.0 <1.3.0 || >=1.3.0 <1.4.0").toNormalizedString());
		assertEquals(">=1.2.0 <1.4.0", VersionRange.create(">=1.3.0 <1.4.0 || >=1.2.0 <1.3.0").toNormalizedString());
		assertEquals(">=1.2.0 <1.4.0", VersionRange.create(">=1.3.1 <1.4.0 || >=1.2.0 <=1.3.0").toNormalizedString());
		assertEquals(">=1.2.0 <1.4.0", VersionRange.create(">=1.2.0 <=1.3.0 || >=1.3.1 <1.4.0").toNormalizedString());
	}

	@Test
	public void union2() {
		VersionRange range = VersionRange.create(">1.2.0 <=1.3.0").merge(VersionRange.create(">1.3.0 <=1.4.0"));
		assertEquals(">1.2.0 <=1.4.0", range.toNormalizedString());
	}

	@Test
	public void isOverlap() {
		assertTrue(VersionRange.create(">1.2.0 <=1.3.0").isOverlap(VersionRange.create(">=1.3.0 <=1.4.0")));
		assertFalse(VersionRange.create(">1.2.0 <1.3.0").isOverlap(VersionRange.create(">=1.3.0 <=1.4.0")));
		assertFalse(VersionRange.create(">1.2.0 <=1.3.0").isOverlap(VersionRange.create(">1.3.0 <=1.4.0")));
		assertFalse(VersionRange.create("1.2.x || 1.4.x").isOverlap(VersionRange.create("1.3.x")));
	}

	@Test
	public void and() {
		assertEquals(VersionRange.EMPTY_RANGE, VersionRange.create(">1.2.0 <1.1.0"));
	}

	@Test
	public void findBestMatch() {
		VersionRange range = VersionRange.create(">1.2.0 <=1.3.0");
		assertEquals("1.2.7", range.findBestMatch(asList(
				Version.create("1.4.2"),
				Version.create("1.2.8-alpha"),
				Version.create("1.2.3"),
				Version.create("1.2.7"),
				Version.create("1.2.5")
		)).toString());
	}

	@Test
	public void less() {
		try {
			VersionRange range = VersionRange.create("<1.2.0");
			assertEquals(VersionRange.less(Version.create("1.2.0")), range);
			assertEquals(Version.create("1.2.0"), range.getMaxVersion());
			assertEquals(Version.MIN, range.getMinVersion());
			assertTrue(range.isExcludeEnd());

			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 1, 9, "alpha")));
			assertTrue(range.includes(Version.create(1, 1, 9)));
			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 2, 0)));

			range = VersionRange.create("<1.2.0--");
			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 1, 9, "alpha")));
			assertFalse(range.includes(null));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void lessEqual() {
		try {
			VersionRange range = VersionRange.create("<=1.2.0");
			assertEquals(VersionRange.lessOrEqual(Version.create("1.2.0")), range);
			assertFalse(range.isExcludeEnd());

			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 1, 9, "alpha")));
			assertTrue(range.includes(Version.create(1, 1, 9)));
			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 0)));

			range = VersionRange.create("<=1.2.0--");
			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 0, "-")));
			assertFalse(range.includes(Version.create(1, 1, 9, "alpha")));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void majorMinorX() {
		try {
			VersionRange range = VersionRange.create("1.2.x");
			assertFalse(range.includes(Version.create(1, 1, 9)));
			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 0)));
			assertTrue(range.includes(Version.create(1, 2, 2)));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void majorX() {
		try {
			VersionRange range = VersionRange.create("1.x");
			assertFalse(range.includes(Version.create(0, 9, 9)));
			assertFalse(range.includes(Version.create(2, 0, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 0, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 0, 0)));
			assertTrue(range.includes(Version.create(1, 0, 2)));
			assertTrue(range.includes(Version.create(1, 2, 0)));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void specific() {
		try {
			VersionRange range = VersionRange.exact(Version.create(1, 0, 0));
			assertFalse(range.includes(Version.create(0, 9, 9)));
			assertFalse(range.includes(Version.create(1, 0, 1)));
			assertFalse(range.includes(Version.create(1, 0, 0, "alpha")));
			assertTrue(range.includes(Version.create("1.0.0")));

			range = VersionRange.create("1.0.0");
			assertFalse(range.includes(Version.create(0, 9, 9)));
			assertFalse(range.includes(Version.create(1, 0, 1)));
			assertFalse(range.includes(Version.create(1, 0, 0, "alpha")));
			assertTrue(range.includes(Version.create("1.0.0")));

			range = VersionRange.create(">=1.0.0 <=1.0.0");
			assertFalse(range.includes(Version.create(0, 9, 9)));
			assertFalse(range.includes(Version.create(1, 0, 1)));
			assertFalse(range.includes(Version.create(1, 0, 0, "alpha")));
			assertTrue(range.includes(Version.create("1.0.0")));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void tildeVersions() {
		try {
			VersionRange range = VersionRange.create("~1.2.3");
			assertFalse(range.includes(Version.create(1, 2, 3, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 3)));
			assertTrue(range.includes(Version.create(1, 2, 10)));

			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 3, 0)));

			range = VersionRange.create("~1.2");
			assertFalse(range.includes(Version.create(1, 2, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 2, 0)));
			assertTrue(range.includes(Version.create(1, 2, 10)));

			assertFalse(range.includes(Version.create(1, 3, 0, "alpha")));
			assertFalse(range.includes(Version.create(1, 3, 0)));

			range = VersionRange.create("~1");
			assertFalse(range.includes(Version.create(1, 0, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 0, 0)));
			assertTrue(range.includes(Version.create(1, 0, 10)));

			assertFalse(range.includes(Version.create(1, 1, 0, "alpha")));
			assertTrue(range.includes(Version.create(1, 1, 0)));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void rangeAboveVersion() {
		List<List<String>> pairs = (List<List<String>>)Pcore.typeEvaluator().resolve("[\n" +
				"['~1.2.2', '1.2.1'],\n" +
				"['~0.6.1-1', '0.6.1-0'],\n" +
				"['1.0.0 - 2.0.0', '0.0.1'],\n" +
				"['1.0.0-beta.2', '1.0.0-beta.1'],\n" +
				"['1.0.0', '0.0.0'],\n" +
				"['>=2.0.0', '1.1.1'],\n" +
				"['>=2.0.0', '1.2.9'],\n" +
				"['>2.0.0', '2.0.0'],\n" +
				"['0.1.20 || 1.2.4', '0.1.5'],\n" +
				"['2.x.x', '1.0.0'],\n" +
				"['1.2.x', '1.1.0'],\n" +
				"['1.2.x || 2.x', '1.0.0'],\n" +
				"['2.*.*', '1.0.1'],\n" +
				"['1.2.*', '1.1.3'],\n" +
				"['1.2.* || 2.*', '1.1.9999'],\n" +
				"['2', '1.0.0'],\n" +
				"['2.3', '2.2.2'],\n" +
				"['~2.4', '2.3.0'],\n" +
				"['~2.4', '2.3.5'],\n" +
				"['~>3.2.1', '3.2.0'],\n" +
				"['~1', '0.2.3'],\n" +
				"['~>1', '0.2.4'],\n" +
				"['~> 1', '0.2.3'],\n" +
				"['~1.0', '0.1.2'],\n" +
				"['~ 1.0', '0.1.0'],\n" +
				"['>1.2', '1.2.0'],\n" +
				"['> 1.2', '1.2.1'],\n" +
				"['1', '0.0.0-beta'],\n" +
				"['~v0.5.4-pre', '0.5.4-alpha'],\n" +
				"['~v0.5.4-pre', '0.5.4-alpha'],\n" +
				"['=0.7.x', '0.6.0'],\n" +
				"['=0.7.x', '0.6.0-asdf'],\n" +
				"['>=0.7.x', '0.6.0'],\n" +
				"['~1.2.2', '1.2.1'],\n" +
				"['1.0.0 - 2.0.0', '0.2.3'],\n" +
				"['1.0.0', '0.0.1'],\n" +
				"['>=2.0.0', '1.0.0'],\n" +
				"['>=2.0.0', '1.9999.9999'],\n" +
				"['>=2.0.0', '1.2.9'],\n" +
				"['>2.0.0', '2.0.0'],\n" +
				"['>2.0.0', '1.2.9'],\n" +
				"['2.x.x', '1.1.3'],\n" +
				"['1.2.x', '1.1.3'],\n" +
				"['1.2.x || 2.x', '1.1.3'],\n" +
				"['2.*.*', '1.1.3'],\n" +
				"['1.2.*', '1.1.3'],\n" +
				"['1.2.* || 2.*', '1.1.3'],\n" +
				"['2', '1.9999.9999'],\n" +
				"['2.3', '2.2.1'],\n" +
				"['~2.4', '2.3.0'],\n" +
				"['~>3.2.1', '2.3.2'],\n" +
				"['~1', '0.2.3'],\n" +
				"['~>1', '0.2.3'],\n" +
				"['~1.0', '0.0.0'],\n" +
				"['>1', '1.0.0'],\n" +
				"['2', '1.0.0-beta'],\n" +
				"['>1', '1.0.0-beta'],\n" +
				"['> 1', '1.0.0-beta'],\n" +
				"['=0.7.x', '0.6.2'],\n" +
				"['=0.7.x', '0.7.0-asdf'],\n" +
				"['^1', '1.0.0-0'],\n" +
				"['>=0.7.x', '0.7.0-asdf'],\n" +
				"['1', '1.0.0-beta'],\n" +
				"['>=0.7.x', '0.6.2']]");
		for(List<String> pair : pairs) {
			VersionRange vr = VersionRange.create(pair.get(0));
			Version v = Version.create(pair.get(1));
			assertTrue(vr.isAbove(v), String.format("Range %s(%s) is above version %s", vr, vr.toNormalizedString(), v));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void rangeNotAboveVersion() {
		List<List<String>> pairs = (List<List<String>>)Pcore.typeEvaluator().resolve("[\n" +
				"['~ 1.0', '1.1.0'],\n" +
				"['~0.6.1-1', '0.6.1-1'],\n" +
				"['1.0.0 - 2.0.0', '1.2.3'],\n" +
				"['1.0.0 - 2.0.0', '2.9.9'],\n" +
				"['1.0.0', '1.0.0'],\n" +
				"['>=*', '0.2.4'],\n" +
				"['', '1.0.0'],\n" +
				"['*', '1.2.3'],\n" +
				"['>=1.0.0', '1.0.0'],\n" +
				"['>=1.0.0', '1.0.1'],\n" +
				"['>=1.0.0', '1.1.0'],\n" +
				"['>1.0.0', '1.0.1'],\n" +
				"['>1.0.0', '1.1.0'],\n" +
				"['<=2.0.0', '2.0.0'],\n" +
				"['<=2.0.0', '1.9999.9999'],\n" +
				"['<=2.0.0', '0.2.9'],\n" +
				"['<2.0.0', '1.9999.9999'],\n" +
				"['<2.0.0', '0.2.9'],\n" +
				"['>= 1.0.0', '1.0.0'],\n" +
				"['>=  1.0.0', '1.0.1'],\n" +
				"['>=   1.0.0', '1.1.0'],\n" +
				"['> 1.0.0', '1.0.1'],\n" +
				"['>  1.0.0', '1.1.0'],\n" +
				"['<=   2.0.0', '2.0.0'],\n" +
				"['<= 2.0.0', '1.9999.9999'],\n" +
				"['<=  2.0.0', '0.2.9'],\n" +
				"['<    2.0.0', '1.9999.9999'],\n" +
				"[\"<\\t2.0.0\", '0.2.9'],\n" +
				"['>=0.1.97', '0.1.97'],\n" +
				"['0.1.20 || 1.2.4', '1.2.4'],\n" +
				"['0.1.20 || >1.2.4', '1.2.4'],\n" +
				"['0.1.20 || 1.2.4', '1.2.3'],\n" +
				"['0.1.20 || 1.2.4', '0.1.20'],\n" +
				"['>=0.2.3 || <0.0.1', '0.0.0'],\n" +
				"['>=0.2.3 || <0.0.1', '0.2.3'],\n" +
				"['>=0.2.3 || <0.0.1', '0.2.4'],\n" +
				"['||', '1.3.4'],\n" +
				"['2.x.x', '2.1.3'],\n" +
				"['1.2.x', '1.2.3'],\n" +
				"['1.2.x || 2.x', '2.1.3'],\n" +
				"['1.2.x || 2.x', '1.2.3'],\n" +
				"['x', '1.2.3'],\n" +
				"['2.*.*', '2.1.3'],\n" +
				"['1.2.*', '1.2.3'],\n" +
				"['1.2.* || 2.*', '2.1.3'],\n" +
				"['1.2.* || 2.*', '1.2.3'],\n" +
				"['1.2.* || 2.*', '1.2.3'],\n" +
				"['*', '1.2.3'],\n" +
				"['2', '2.1.2'],\n" +
				"['2.3', '2.3.1'],\n" +
				"['~2.4', '2.4.0'],\n" +
				"['~2.4', '2.4.5'],\n" +
				"['~>3.2.1', '3.2.2'],\n" +
				"['~1', '1.2.3'],\n" +
				"['~>1', '1.2.3'],\n" +
				"['~> 1', '1.2.3'],\n" +
				"['~1.0', '1.0.2'],\n" +
				"['~ 1.0', '1.0.2'],\n" +
				"['>=1', '1.0.0'],\n" +
				"['>= 1', '1.0.0'],\n" +
				"['<1.2', '1.1.1'],\n" +
				"['< 1.2', '1.1.1'],\n" +
				"['~v0.5.4-pre', '0.5.5'],\n" +
				"['~v0.5.4-pre', '0.5.4'],\n" +
				"['=0.7.x', '0.7.2'],\n" +
				"['>=0.7.x', '0.7.2'],\n" +
				"['<=0.7.x', '0.6.2'],\n" +
				"['>0.2.3 >0.2.4 <=0.2.5', '0.2.5'],\n" +
				"['>=0.2.3 <=0.2.4', '0.2.4'],\n" +
				"['1.0.0 - 2.0.0', '2.0.0'],\n" +
				"['^3.0.0', '4.0.0'],\n" +
				"['^1.0.0 || ~2.0.1', '2.0.0'],\n" +
				"['^0.1.0 || ~3.0.1 || 5.0.0', '3.2.0'],\n" +
				"['^0.1.0 || ~3.0.1 || 5.0.0', '1.0.0-beta'],\n" +
				"['^0.1.0 || ~3.0.1 || 5.0.0', '5.0.0-0'],\n" +
				"['^0.1.0 || ~3.0.1 || >4 <=5.0.0', '3.5.0'],\n" +
				"['^1.0.0-alpha', '1.0.0-beta'],\n" +
				"['~1.0.0-alpha', '1.0.0-beta'],\n" +
				"['=0.1.0', '1.0.0']]");
		for(List<String> pair : pairs) {
			VersionRange vr = VersionRange.create(pair.get(0));
			Version v = Version.create(pair.get(1));
			assertFalse(vr.isAbove(v), String.format("Range %s(%s) is not above version %s", vr, vr.toNormalizedString(), v));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void rangeBelowVersion() {
		List<List<String>> pairs = (List<List<String>>)Pcore.typeEvaluator().resolve("[\n" +
				"['~1.2.2', '1.3.0'],\n" +
				"['~0.6.1-1', '0.7.1-1'],\n" +
				"['1.0.0 - 2.0.0', '2.0.1'],\n" +
				"['1.0.0', '1.0.1-beta1'],\n" +
				"['1.0.0', '2.0.0'],\n" +
				"['<=2.0.0', '2.1.1'],\n" +
				"['<=2.0.0', '3.2.9'],\n" +
				"['<2.0.0', '2.0.0'],\n" +
				"['0.1.20 || 1.2.4', '1.2.5'],\n" +
				"['2.x.x', '3.0.0'],\n" +
				"['1.2.x', '1.3.0'],\n" +
				"['1.2.x || 2.x', '3.0.0'],\n" +
				"['2.*.*', '5.0.1'],\n" +
				"['1.2.*', '1.3.3'],\n" +
				"['1.2.* || 2.*', '4.0.0'],\n" +
				"['2', '3.0.0'],\n" +
				"['2.3', '2.4.2'],\n" +
				"['~2.4', '2.5.0'],\n" +
				"['~2.4', '2.5.5'],\n" +
				"['~>3.2.1', '3.3.0'],\n" +
				"['~1', '2.2.3'],\n" +
				"['~>1', '2.2.4'],\n" +
				"['~> 1', '3.2.3'],\n" +
				"['~1.0', '1.1.2'],\n" +
				"['~ 1.0', '1.1.0'],\n" +
				"['<1.2', '1.2.0'],\n" +
				"['< 1.2', '1.2.1'],\n" +
				"['1', '2.0.0-beta'],\n" +
				"['~v0.5.4-pre', '0.6.0'],\n" +
				"['~v0.5.4-pre', '0.6.1-pre'],\n" +
				"['=0.7.x', '0.8.0'],\n" +
				"['=0.7.x', '0.8.0-asdf'],\n" +
				"['<0.7.x', '0.7.0'],\n" +
				"['~1.2.2', '1.3.0'],\n" +
				"['1.0.0 - 2.0.0', '2.2.3'],\n" +
				"['1.0.0', '1.0.1'],\n" +
				"['<=2.0.0', '3.0.0'],\n" +
				"['<=2.0.0', '2.9999.9999'],\n" +
				"['<=2.0.0', '2.2.9'],\n" +
				"['<2.0.0', '2.9999.9999'],\n" +
				"['<2.0.0', '2.2.9'],\n" +
				"['2.x.x', '3.1.3'],\n" +
				"['1.2.x', '1.3.3'],\n" +
				"['1.2.x || 2.x', '3.1.3'],\n" +
				"['2.*.*', '3.1.3'],\n" +
				"['1.2.*', '1.3.3'],\n" +
				"['1.2.* || 2.*', '3.1.3'],\n" +
				"['2', '3.1.2'],\n" +
				"['2.3', '2.4.1'],\n" +
				"['~2.4', '2.5.0'],\n" +
				"['~>3.2.1', '3.3.2'],\n" +
				"['~1', '2.2.3'],\n" +
				"['~>1', '2.2.3'],\n" +
				"['~1.0', '1.1.0'],\n" +
				"['<1', '1.0.0'],\n" +
				"['1', '2.0.0-beta'],\n" +
				"['<1', '1.0.0-beta'],\n" +
				"['< 1', '1.0.0-beta'],\n" +
				"['=0.7.x', '0.8.2'],\n" +
				"['<0.7.x', '0.7.2']]");
		for(List<String> pair : pairs) {
			VersionRange vr = VersionRange.create(pair.get(0));
			Version v = Version.create(pair.get(1));
			assertTrue(vr.isBelow(v), String.format("Range %s(%s) is below version %s", vr, vr.toNormalizedString(), v));
		}
	}


	@SuppressWarnings("unchecked")
	@Test
	public void rangeNotBelowVersion() {
		List<List<String>> pairs = (List<List<String>>)Pcore.typeEvaluator().resolve("[\n" +
				"['~0.6.1-1', '0.6.1-1'],\n" +
				"['1.0.0 - 2.0.0', '1.2.3'],\n" +
				"['1.0.0 - 2.0.0', '0.9.9'],\n" +
				"['1.0.0', '1.0.0'],\n" +
				"['>=*', '0.2.4'],\n" +
				"['', '1.0.0'],\n" +
				"['*', '1.2.3'],\n" +
				"['*', '1.2.3-foo'],\n" +
				"['>=1.0.0', '1.0.0'],\n" +
				"['>=1.0.0', '1.0.1'],\n" +
				"['>=1.0.0', '1.1.0'],\n" +
				"['>1.0.0', '1.0.1'],\n" +
				"['>1.0.0', '1.1.0'],\n" +
				"['<=2.0.0', '2.0.0'],\n" +
				"['<=2.0.0', '1.9999.9999'],\n" +
				"['<=2.0.0', '0.2.9'],\n" +
				"['<2.0.0', '1.9999.9999'],\n" +
				"['<2.0.0', '0.2.9'],\n" +
				"['>= 1.0.0', '1.0.0'],\n" +
				"['>=  1.0.0', '1.0.1'],\n" +
				"['>=   1.0.0', '1.1.0'],\n" +
				"['> 1.0.0', '1.0.1'],\n" +
				"['>  1.0.0', '1.1.0'],\n" +
				"['<=   2.0.0', '2.0.0'],\n" +
				"['<= 2.0.0', '1.9999.9999'],\n" +
				"['<=  2.0.0', '0.2.9'],\n" +
				"['<    2.0.0', '1.9999.9999'],\n" +
				"[\"<\\t2.0.0\", '0.2.9'],\n" +
				"['>=0.1.97', '0.1.97'],\n" +
				"['>=0.1.97', '0.1.97'],\n" +
				"['0.1.20 || 1.2.4', '1.2.4'],\n" +
				"['0.1.20 || >1.2.4', '1.2.4'],\n" +
				"['0.1.20 || 1.2.4', '1.2.3'],\n" +
				"['0.1.20 || 1.2.4', '0.1.20'],\n" +
				"['>=0.2.3 || <0.0.1', '0.0.0'],\n" +
				"['>=0.2.3 || <0.0.1', '0.2.3'],\n" +
				"['>=0.2.3 || <0.0.1', '0.2.4'],\n" +
				"['||', '1.3.4'],\n" +
				"['2.x.x', '2.1.3'],\n" +
				"['1.2.x', '1.2.3'],\n" +
				"['1.2.x || 2.x', '2.1.3'],\n" +
				"['1.2.x || 2.x', '1.2.3'],\n" +
				"['x', '1.2.3'],\n" +
				"['2.*.*', '2.1.3'],\n" +
				"['1.2.*', '1.2.3'],\n" +
				"['1.2.* || 2.*', '2.1.3'],\n" +
				"['1.2.* || 2.*', '1.2.3'],\n" +
				"['1.2.* || 2.*', '1.2.3'],\n" +
				"['*', '1.2.3'],\n" +
				"['2', '2.1.2'],\n" +
				"['2.3', '2.3.1'],\n" +
				"['~2.4', '2.4.0'],\n" +
				"['~2.4', '2.4.5'],\n" +
				"['~>3.2.1', '3.2.2'],\n" +
				"['~1', '1.2.3'],\n" +
				"['~>1', '1.2.3'],\n" +
				"['~> 1', '1.2.3'],\n" +
				"['~1.0', '1.0.2'],\n" +
				"['~ 1.0', '1.0.2'],\n" +
				"['>=1', '1.0.0'],\n" +
				"['>= 1', '1.0.0'],\n" +
				"['<1.2', '1.1.1'],\n" +
				"['< 1.2', '1.1.1'],\n" +
				"['1', '1.0.0-beta'],\n" +
				"['~v0.5.4-pre', '0.5.5'],\n" +
				"['~v0.5.4-pre', '0.5.4'],\n" +
				"['=0.7.x', '0.7.2'],\n" +
				"['>=0.7.x', '0.7.2'],\n" +
				"['=0.7.x', '0.7.0-asdf'],\n" +
				"['>=0.7.x', '0.7.0-asdf'],\n" +
				"['<=0.7.x', '0.6.2'],\n" +
				"['>0.2.3 >0.2.4 <=0.2.5', '0.2.5'],\n" +
				"['>=0.2.3 <=0.2.4', '0.2.4'],\n" +
				"['1.0.0 - 2.0.0', '2.0.0'],\n" +
				"['^1', '0.0.0-0'],\n" +
				"['^3.0.0', '2.0.0'],\n" +
				"['^1.0.0 || ~2.0.1', '2.0.0'],\n" +
				"['^0.1.0 || ~3.0.1 || 5.0.0', '3.2.0'],\n" +
				"['^0.1.0 || ~3.0.1 || 5.0.0', '1.0.0-beta'],\n" +
				"['^0.1.0 || ~3.0.1 || 5.0.0', '5.0.0-0'],\n" +
				"['^0.1.0 || ~3.0.1 || >4 <=5.0.0', '3.5.0']]");
		for(List<String> pair : pairs) {
			VersionRange vr = VersionRange.create(pair.get(0));
			Version v = Version.create(pair.get(1));
			assertFalse(vr.isBelow(v), String.format("Range %s(%s) is not below version %s", vr, vr.toNormalizedString(), v));
		}
	}
}
