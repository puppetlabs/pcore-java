package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("Tuple Type")
public class TupleTypeTest {
	@Nested
	@DisplayName("computes assignability such that")
	class Assignability {
		@Test
		@DisplayName("a tuple with elements is assignable to the default tuple")
		public void test1() {
			assertTrue(tupleType().isAssignable(tupleType(singletonList(stringType()))));
		}

		@Test
		@DisplayName("a tuple with elements is not assignable to the empty tuple")
		public void test2() {
			assertFalse(tupleType(emptyList()).isAssignable(tupleType(singletonList(stringType()))));
		}

		@Test
		@DisplayName("a tuple with elements is assignable from to a type with assignable elements")
		public void test3() {
			assertTrue(tupleType(singletonList(scalarType())).isAssignable(tupleType(singletonList(stringType()))));
		}

		@Test
		@DisplayName("a tuple with elements is assignable from to a type with unassignable elements")
		public void test4() {
			assertFalse(tupleType(singletonList(stringType())).isAssignable(tupleType(singletonList(scalarType()))));
		}

		@Test
		@DisplayName("a tuple with optional elements not assignable from to a tuple without elements")
		public void test5() {
			assertTrue(tupleType(singletonList(scalarType()), 0, 4).isAssignable(tupleType(emptyList())));
		}

		@Test
		@DisplayName("a tuple with required elements not assignable from to a tuple without elements")
		public void test6() {
			assertFalse(tupleType(singletonList(scalarType()), 1, 4).isAssignable(tupleType(emptyList(), 1, 4)));
		}

		@Test
		@DisplayName("the default array is assignable to the default tuple")
		public void test7() {
			assertTrue(tupleType().isAssignable(arrayType()));
		}

		@Test
		@DisplayName("the default array is not assignable to a tuple with elements")
		public void test8() {
			assertFalse(tupleType(singletonList(scalarType())).isAssignable(arrayType()));
		}

		@Test
		@DisplayName("tuple with elements can be assigned from an array with assignable element type")
		public void test9() {
			assertTrue(tupleType(singletonList(scalarType())).isAssignable(arrayType(stringType(), 1, 1)));
		}

		@Test
		@DisplayName("tuple with elements can not be assigned from an array with unassignable element type")
		public void test10() {
			assertFalse(tupleType(singletonList(stringType())).isAssignable(arrayType(scalarType(), 1, 1)));
		}

		@Test
		@DisplayName("tuple can not be assignable from arbitrary types")
		public void test11() {
			assertFalse(tupleType().isAssignable(stringType()));
		}
	}

	@Test
	@DisplayName("is not equal to arbitrary types")
	public void test11() {
		//noinspection EqualsBetweenInconvertibleTypes
		assertFalse(tupleType().equals(stringType()));
	}

	@Test
	@DisplayName("can merge two tuples")
	public void test2() {
		assertEquals(
				tupleType(singletonList(stringType())).common(tupleType(singletonList(integerType()))),
				arrayType(scalarDataType()));
	}
}
