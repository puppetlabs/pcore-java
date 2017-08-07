package com.puppet.pcore.impl.types;

import com.puppet.pcore.Binary;
import com.puppet.pcore.TypeAssertionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("Array Type")
public class ArrayTypeTest {
	@Nested
	@DisplayName("computes assignability such that")
	class Assignability {
		@Test
		@DisplayName("Array and Tuple are assignable to Array")
		public void toAny() {
			assertTrue(all(asList(tupleType(), arrayType()), type -> arrayType().isAssignable(type)));
		}
	}

	@Nested
	@DisplayName("newInstance")
	class NewInstance {
		@Test
		@DisplayName("An instance of Array can be created as a wrapper of null")
		public void wrapNull() {
			assertEquals(arrayType().newInstance(null, true), singletonList(null));
		}

		@Test
		@DisplayName("An instance of Array can be created as a wrapper of an array")
		public void wrapArray() {
			assertEquals(arrayType().newInstance(asList(1, 2, 3), true), singletonList(asList(1, 2, 3)));
		}

		@Test
		@DisplayName("An instance of Array can be from an array")
		public void copyArray() {
			assertEquals(arrayType().newInstance(asList(1, 2, 3)), asList(1, 2, 3));
		}

		@Test
		@DisplayName("An instance of Array can be created as a wrapper of a string")
		public void wrapString() {
			assertEquals(arrayType().newInstance("hello", true), singletonList("hello"));
		}

		@Test
		@DisplayName("An instance of Array can be created as from a string")
		public void expandString() {
			assertEquals(arrayType().newInstance("hello"), asList("h", "e", "l", "l", "o"));
		}

		@Test
		@DisplayName("An instance of Array can be created as from a binary")
		public void expandBinary() {
			assertEquals(arrayType().newInstance(new Binary(new byte[] { 'h', 'e', 'l', 'l', 'o', })),
					asList((byte)'h', (byte)'e', (byte)'l', (byte)'l', (byte)'o'));
		}

		@Test
		@DisplayName("no arguments yields exception")
		public void noArguments() {
			Throwable ex = assertThrows(TypeAssertionException.class, () -> arrayType().newInstance());
			assertEquals("The factory that creates instances of type 'Array' expects a Tuple[Any, Boolean, 1, 2] value, got Array[0, 0]", ex.getMessage());
		}


		@Test
		@DisplayName("too many arguments yields exception")
		public void tooManyArguments() {
			Throwable ex = assertThrows(TypeAssertionException.class, () -> arrayType().newInstance(false, true, 3));
			assertEquals("The factory that creates instances of type 'Array' expects attribute count to be between 1 and 2, got 3", ex.getMessage());
		}

		@Test
		@DisplayName("incorrect arguments yields exception")
		public void incorrectArguments() {
			Throwable ex = assertThrows(TypeAssertionException.class, () -> arrayType().newInstance(3, 3));
			assertEquals("The factory that creates instances of type 'Array' index '1' expects a Boolean value, got Integer", ex.getMessage());
		}
	}
}
