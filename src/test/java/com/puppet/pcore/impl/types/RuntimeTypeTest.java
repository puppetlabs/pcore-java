package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.regexpType;
import static com.puppet.pcore.impl.types.TypeFactory.runtimeType;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("Runtime Type")
public class RuntimeTypeTest {
	@Test
	@DisplayName("qualified with runtime is assignable to default")
	public void runtime1() {
		assertTrue(runtimeType().isAssignable(runtimeType("lang", "Some::Class")));
	}

	@Test
	@DisplayName("qualified with runtime is assignable to the same runtime")
	public void runtime2() {
		assertTrue(runtimeType("lang", null).isAssignable(runtimeType("lang", "Some::Class")));
	}

	@Test
	@DisplayName("qualified with runtime is assignable to the same runtime with pattern")
	public void runtime3() {
		assertTrue(runtimeType("lang", null).isAssignable(runtimeType("lang", "Some::Class", regexpType("foo"))));
	}

	@Test
	@DisplayName("qualified with runtime, class, and pattern is not assignable to the same runtime with class and no pattern")
	public void runtime4() {
		assertFalse(runtimeType("lang", "Some::Class").isAssignable(runtimeType("lang", "Some::Class", regexpType("foo"))));
	}

	@Test
	@DisplayName("qualified with runtime, class, and pattern is assignable to the same runtime with class and pattern")
	public void runtime5() {
		assertTrue(runtimeType("lang", "Some::Class", regexpType("foo")).isAssignable(runtimeType("lang", "Some::Class", regexpType("foo"))));
	}

	@Test
	@DisplayName("can use runtime class to compute instance of")
	public void runtime6() {
		assertTrue(runtimeType("java", "java.lang.String").isInstance("string"));
	}

	@Test
	@DisplayName("can use runtime class to compute instance of super")
	public void runtime7() {
		assertTrue(runtimeType("java", "java.lang.Number").isInstance(12));
	}

	@Test
	@DisplayName("can use runtime class to compute assignability")
	public void runtime8() {
		assertTrue(runtimeType("java", "java.lang.Number").isAssignable(runtimeType("java", "java.lang.Integer")));
	}

	@Test
	@DisplayName("can compute common runtime from two runtime classes")
	public void runtime9() {
		assertEquals(runtimeType("java", "java.lang.Long").common(runtimeType("java", "java.lang.Double")), runtimeType("java", "java.lang.Number"));
	}
}
