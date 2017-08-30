package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("Data Type")
public class DataTypeTest {
	@Nested
	@DisplayName("computes assignability such that")
	class Assignability {
		@Test
		@DisplayName("Undef is assignable to Data")
		public void undefToData() {
			assertTrue(dataType().isAssignable(undefType()));
		}

		@Test
		@DisplayName("Boolean is assignable to Data")
		public void booleanToData() {
			assertTrue(dataType().isAssignable(booleanType()));
		}

		@Test
		@DisplayName("Float is assignable to Data")
		public void floatToData() {
			assertTrue(dataType().isAssignable(floatType()));
		}

		@Test
		@DisplayName("Integer is assignable to Data")
		public void integerToData() {
			assertTrue(dataType().isAssignable(integerType()));
		}

		@Test
		@DisplayName("String is assignable to Data")
		public void stringToData() {
			assertTrue(dataType().isAssignable(stringType()));
		}

		@Test
		@DisplayName("Pattern is assignable to Data")
		public void patternToData() {
			assertTrue(dataType().isAssignable(patternType(regexpType(".*"))));
		}

		@Test
		@DisplayName("Optional[String] is assignable to Data")
		public void optionalStringToData() {
			assertTrue(dataType().isAssignable(optionalType(stringType())));
		}

		@Test
		@DisplayName("Optional[Data] is assignable to Data")
		public void optionalDataToData() {
			assertTrue(dataType().isAssignable(optionalType(dataType())));
		}
	}
}
