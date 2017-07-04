package com.puppet.pcore.loader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.Constants.RUNTIME_NAME_AUTHORITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TypedName")
public class TypedNameTest {
	@Test
	@DisplayName("Leading :: will be stripped off")
	void stripLeadingDoubleColon() {
		TypedName tn = new TypedName("type", "::Foo::Bar");
		assertEquals("Foo::Bar", tn.name);
		assertEquals(RUNTIME_NAME_AUTHORITY + "/type/" + tn.name, tn.toString());
		assertTrue(tn.isQualified());
	}

	@Test
	@DisplayName("Qualified names are considered qualified")
	void qualifiedIsQualified() {
		TypedName tn = new TypedName("type", "Foo::Bar");
		assertTrue(tn.isQualified());
	}

	@Test
	@DisplayName("Unqualified names are not considered qualified")
	void unqualifiedIsNotQualified() {
		TypedName tn = new TypedName("type", "Foo");
		assertFalse(tn.isQualified());
	}

	@Test
	@DisplayName("toString() returns <name authority>/<type>/<name>")
	void toStringIncludes() {
		TypedName tn = new TypedName("type", "Foo::Bar");
		assertEquals(RUNTIME_NAME_AUTHORITY + "/type/" + tn.name, tn.toString());
	}

	@Test
	@DisplayName("compareTo is case insensitive")
	void caseInsensitiveCompareTo() {
		assertEquals(0, new TypedName("type", "A::B").compareTo(new TypedName("type", "a::b")));
		assertEquals(-1, new TypedName("type", "A::B").compareTo(new TypedName("type", "a::c")));
		assertEquals(1, new TypedName("type", "A::C").compareTo(new TypedName("type", "a::b")));
	}

	@Test
	@DisplayName("equals is case insensitive")
	void caseInsensitiveEquals() {
		assertTrue(new TypedName("type", "A::B").equals(new TypedName("type", "a::b")));
	}

	@Test
	@DisplayName("hash is case insensitive")
	void caseInsensitiveHash() {
		assertTrue(new TypedName("type", "A::B").hashCode() == new TypedName("type", "a::b").hashCode());
	}
}
