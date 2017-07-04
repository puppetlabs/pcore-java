package com.puppet.pcore.semver.tests;

import com.puppet.pcore.semver.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Version.
 */
public class VersionTest {
	@Test
	public void badNegativeNumbers() {
		try {
			Version.create(1, 0, -1);
			fail("should not create version with negative numbers");
		} catch(IllegalArgumentException ignored) {
		}
	}

	@Test
	public void zeroStartingNumbers() {
		try {
			Version.create("0.0.02");
			fail("should not create version numbers starting with zero");
		} catch(IllegalArgumentException ignored) {
		}
	}

	@Test
	public void badPreReleaseSeparator() {
		try {
			Version.create("0.0.0.alpha");
			fail("should not permit '.' as pre-release separator");
		} catch(IllegalArgumentException ignored) {
		}
	}

	@Test
	public void badPreReleaseString() {
		try {
			Version.create("0.0.0-bad=qualifier");
			fail("should not create version illegal characters in pre-release");
		} catch(IllegalArgumentException ignored) {
		}
	}

	@Test
	public void numbersMagnitude() {
		try {
			assertTrue(Version.create("0.0.1").compareTo(Version.create("0.0.2")) < 0);
			assertTrue(Version.create("0.0.9").compareTo(Version.create("0.1.0")) < 0);
			assertTrue(Version.create("0.9.9").compareTo(Version.create("1.0.0")) < 0);
			assertTrue(Version.create("1.9.0").compareTo(Version.create("1.10.0")) < 0);
			assertTrue(Version.create("1.10.0").compareTo(Version.create("1.11.0")) < 0);
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void okStringVersions() {
		try {
			assertEquals(Version.create(0, 0, 0), Version.create("0.0.0"));
			assertEquals(Version.create(0, 0, 1), Version.create("0.0.1"));
			assertEquals(Version.create(0, 1, 0), Version.create("0.1.0"));
			assertEquals(Version.create(1, 0, 0), Version.create("1.0.0"));
			assertEquals(Version.create(0, 0, 0, "alpha", null), Version.create("0.0.0-alpha"));
			assertEquals(Version.create(0, 0, 1, "alpha", null), Version.create("0.0.1-alpha"));
			assertEquals(Version.create(0, 1, 0, "alpha", null), Version.create("0.1.0-alpha"));
			assertEquals(Version.create(1, 0, 0, "alpha", null), Version.create("1.0.0-alpha"));
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void preReleaseLessThanVersion() {
		try {
			assertTrue(Version.create("1.0.0").compareTo(Version.create("1.0.0-alpha")) > 0);
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	@DisplayName("Returns individual parts")
	public void testIndividualParts() {
		Version v = Version.create("1.2.3-alpha.10.b+fx.38");
		assertEquals(1, v.getMajor());
		assertEquals(2, v.getMinor());
		assertEquals(3, v.getPatch());
		assertEquals("alpha.10.b", v.getPreRelease());
		assertEquals("fx.38", v.getBuild());
	}

	@Test
	@DisplayName("getPrerelease() returns null if no pre-release")
	public void testNoPrerelease() {
		assertNull(Version.create("1.2.3").getPreRelease());
	}

	@Test
	@DisplayName("getBuilder() returns null if no build")
	public void testNoBuild() {
		assertNull(Version.create("1.2.3").getBuild());
	}

	@Test
	@DisplayName("fromStringOrNull returns null for null")
	public void fromStringOrNullReturnsNull() {
		assertNull(Version.fromStringOrNull(null));
	}

	@Test
	@DisplayName("fromStringOrNull returns parsed version")
	public void fromStringOrNullReturnsVersion() {
		assertEquals(Version.create("1.2.3"), Version.fromStringOrNull("1.2.3"));
	}

	@Test
	@DisplayName("version with pre-release identifiers that start with zero and has characters are valid")
	public void validZeroStart() {
		assertTrue(Version.isValid("1.2.3-00v"));
	}

	@Test
	@DisplayName("version with digit-only pre-release identifiers that start with zero are invalid")
	public void invalidZeroStart() {
		assertTrue(Version.isValid("1.2.3-034"));
	}

	@Test
	@DisplayName("pre-release identifiers may contain dash")
	public void validDash() {
		assertTrue(Version.isValid("1.2.3-00v-alpha"));
	}

	@Test
	public void preReleaseMagnitude() {
		try {
			assertTrue(Version.create("1.0.0-alpha1").compareTo(Version.create("1.0.0-beta1")) < 0);
			assertTrue(Version.create("1.0.0-beta1").compareTo(Version.create("1.0.0-beta2")) < 0);
			assertTrue(Version.create("1.0.0-beta2").compareTo(Version.create("1.0.0-rc1")) < 0);
			assertTrue(Version.create("1.0.0-a.1").compareTo(Version.create("1.0.0-a.a")) < 0);
			assertTrue(Version.create("1.0.0-a.2").compareTo(Version.create("1.0.0-a.1a")) < 0);
			assertTrue(Version.create("1.0.0-3.1").compareTo(Version.create("1.0.0-10.0")) < 0);
		} catch(IllegalArgumentException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void tooFewDigits() {
		try {
			Version.create("0.0");
			fail("should not create version with just two digits");
		} catch(IllegalArgumentException ignored) {
		}
	}
}
