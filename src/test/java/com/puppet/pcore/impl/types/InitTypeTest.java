package com.puppet.pcore.impl.types;

import com.puppet.pcore.PcoreException;
import com.puppet.pcore.Sensitive;
import com.puppet.pcore.TypeAssertionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.puppet.pcore.test.TestHelper.assertMatches;
import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("Init Type")
public class InitTypeTest {
	@Nested
	@DisplayName("computing assignability")
	class Assignability {
		@Test
		@DisplayName("RichData is assignable to Init")
		public void richDataToInit() {
			assertTrue(initType().isAssignable(richDataType()));
		}

		@Test
		@DisplayName("Init is not assignable to RichData")
		public void initToRichData() {
			assertFalse(richDataType().isAssignable(initType()));
		}

		@Test
		@DisplayName("Runtime is not assignable to Init")
		public void runtimeToInit() {
			assertFalse(initType().isAssignable(runtimeType("java.io.File")));
		}

		@Test
		@DisplayName("Init[T1] is assignable to Init[T2] when T1 is assignable to T2")
		public void t1Ift2() {
			assertTrue(initType(numericType()).isAssignable(initType(integerType())));
			assertTrue(initType(arrayType(integerType())).isAssignable(tupleType(singletonList(integerType()))));
		}

		@Test
		@DisplayName("Init[T1] is not assignable to Init[T2] unless T1 is assignable to T2")
		public void t1IfNott2() {
			assertFalse(initType(integerType()).isAssignable(initType(numericType())));
			assertFalse(initType(tupleType(singletonList(integerType()))).isAssignable(initType(arrayType(integerType()))));
		}

		@Test
		@DisplayName("T is assignable to Init[T]")
		public void initTypeToInit() {
			assertTrue(initType(integerType()).isAssignable(integerType()));
		}

		@Test
		@DisplayName("T1 is assignable to Init[T2] if T2 can be created from instance of T1")
		public void initTypeToInitConversion() {
			assertTrue(initType(stringType()).isAssignable(integerType()));
		}
	}

	@Nested
	@DisplayName("computing instance of")
	class InstanceOf {
		@Test
		@DisplayName("RichData is assignable to Init")
		public void richDataToInit() {
			Object richData = asMap("a", asList(1, 2, new Sensitive(3)), 2, Instant.parse("2014-12-01T13:15:00.00Z"));
			assertTrue(initType().isInstance(richData));
		}
	}


		@Nested
	@DisplayName("newInstance")
	class NewInstance {
		@Test
		@DisplayName("on unparameterized Init yields exception")
		public void onUnparameterized() {
			Throwable ex = assertThrows(PcoreException.class, () -> initType().newInstance());
			assertEquals("Creation of new instance of type 'Init' is not supported", ex.getMessage());
		}

		@Test
		@DisplayName("an Init[String] can create an instance from a String")
		public void stringFromString() {
			assertEquals("hello", initType(stringType()).newInstance("hello"));
		}

		@Test
		@DisplayName("an Init[String] can create an instance from parameter tuple [String]")
		public void stringFromParameters_String() {
			assertEquals("hello", initType(stringType()).newInstance(asList("hello")));
		}

		@Test
		@DisplayName("an Init[String, '%d'] can create an instance from an integer")
		public void stringFromExtraAnd_Integer() {
			assertEquals("0x17", initType(stringType(), asList("%#x")).newInstance(23));
		}

		@Test
		@DisplayName("an Init[String] can create an instance from an integer and a format")
		public void stringFrom_Integer_String() {
			assertEquals("0x17", initType(stringType()).newInstance(asList(23, "%#x")));
		}

		@Test
		@DisplayName("an Init[Sensitive[String]] can create an instance from a String")
		public void wrapNull() {
			assertEquals(initType(sensitiveType(stringType())).newInstance("256"), new Sensitive("256"));
		}

		@Test
		@DisplayName("no arguments yields exception")
		public void noArguments() {
			Throwable ex = assertThrows(TypeAssertionException.class, () -> initType(integerType()).newInstance());
			assertEquals("The factory that creates instances of type 'Init[Integer]' expects a value of type Tuple[Array] or Tuple[Any], got Array[0, 0]", ex.getMessage());
		}

		@Test
		@DisplayName("too many arguments yields exception")
		public void tooManyArguments() {
			Throwable ex = assertThrows(TypeAssertionException.class, () -> initType(integerType()).newInstance(false, true, 3));
			assertEquals("The factory that creates instances of type 'Init[Integer]' expects attribute count to be 1, got 3", ex.getMessage());
		}

		@Test
		@DisplayName("incorrect arguments yields exception")
		public void incorrectArguments() {
			Throwable ex = assertThrows(PcoreException.class, () -> initType(integerType()).newInstance(typeType()));
			assertMatches("The factory that creates instances of type 'Integer'.*expects a value of type Variant or Boolean, got Type", ex.getMessage());
		}
	}
}
