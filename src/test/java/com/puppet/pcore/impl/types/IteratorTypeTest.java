package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("Iterator Type")
public class IteratorTypeTest {
	@Test
	@DisplayName("responds true to isIterable()")
	public void test1() {
		assertTrue(iteratorType().isIterable(null));
	}

	@Test
	@DisplayName("returns Iterator with generalized element type when generalized")
	public void test2() {
		assertEquals(iteratorType(stringType(0, 10)).generalize(), iteratorType(stringType()));
	}

	@Test
	@DisplayName("considers Iterator with assignable element type as assignable")
	public void test3() {
		assertTrue(iteratorType(stringType()).isAssignable(iteratorType(stringType(1, 10))));
	}

	@Nested
	@DisplayName("when asked for an Iterable type")
	class AskedForIterable {
		@Test
		@DisplayName("the default iterator type returns the default iterable type")
		public void test1() {
			assertEquals(iteratorType().asIterableType(null), iterableType());
		}

		@Test
		@DisplayName("a typed iterator type returns the an equally typed iterable type")
		public void test2() {
			assertEquals(iteratorType(stringType()).asIterableType(null), iterableType(stringType()));
		}
	}
}
