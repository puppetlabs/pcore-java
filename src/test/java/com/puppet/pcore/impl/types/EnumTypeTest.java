package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("Enum Type")
public class EnumTypeTest {
	@Test
	@DisplayName("string is assignable to matching enum")
	public void enum1() {
		assertTrue(enumType("a", "b", "c").isAssignable(stringType("b")));
	}

	@Test
	@DisplayName("string is not assignable to enum that doesn't match")
	public void enum2() {
		assertFalse(enumType("a", "b", "c").isAssignable(stringType("d")));
	}

	@Test
	@DisplayName("enum is assignable to matching enum")
	public void enum3() {
		assertTrue(enumType("a", "b", "c").isAssignable(enumType("a", "b")));
	}

	@Test
	@DisplayName("enum is not assignable to enum that doesn't match")
	public void enum4() {
		assertFalse(enumType("a", "b", "c").isAssignable(enumType("c", "d")));
	}

	@Test
	@DisplayName("enum is not assignable from integer")
	public void enum5() {
		assertFalse(enumType("1", "2", "3").isAssignable(integerType(1, 1)));
	}

	@Test
	@DisplayName("enum is assignable to matching pattern")
	public void pattern2() {
		assertTrue(patternType(regexpType("[abc]+")).isAssignable(enumType("a", "b", "c")));
	}

	@Test
	@DisplayName("common type for two strings is an enum")
	public void commonString() {
		assertEquals(enumType("a", "b"), stringType("a").common(stringType("b")));
	}

	@Test
	@DisplayName("common type for enum and string with value is an enum")
	public void commonEnum1() {
		assertEquals(enumType("a", "b", "c"), enumType("a", "b").common(stringType("c")));
	}

	@Test
	@DisplayName("common type for enum and string with size is an string with size")
	public void commonEnum2() {
		assertEquals(stringType(1, 4), enumType("abc", "b", "cedf").common(stringType(2, 2)));
	}

	@Test
	@DisplayName("common type for two enums is an enum")
	public void commonEnum3() {
		assertEquals(enumType("a", "b"), enumType("a", "b", "c").common(enumType("b", "c")));
	}
}
