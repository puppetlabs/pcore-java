package com.puppet.pcore.impl.types;

import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.TypeEvaluatorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("Any Type")
public class AnyTypeTest {
	@Nested
	@DisplayName("computes assignability such that")
	class Assignability {
		@Test
		@DisplayName("all types are assignable to Any")
		public void toAny() {
			assertTrue(all(TypeEvaluatorImpl.BASIC_TYPES.values(), type -> anyType().isAssignable(type)));
		}

		@Test
		@DisplayName("all types are assignable to themselves")
		public void toSelf() {
			assertTrue(all(TypeEvaluatorImpl.BASIC_TYPES.values(), type -> type.isAssignable(type)));
		}

		@Test
		@DisplayName("no type is assignable from null")
		public void fromNull() {
			assertTrue(all(TypeEvaluatorImpl.BASIC_TYPES.values(), type -> !type.isAssignable(null)));
		}

		@Test
		@DisplayName("all types are assignable to their normalized type")
		public void normalizeTooSelf() {
			assertTrue(all(TypeEvaluatorImpl.BASIC_TYPES.values(), type -> type.normalize().isAssignable(type)));
		}
	}

	@Test
	@DisplayName("all except TypeAlias types generalize to themselves")
	public void generalize() {
		assertTrue(all(TypeEvaluatorImpl.BASIC_TYPES.values(), type -> type.generalize() == type));
	}

	@Test
	@DisplayName("string is assignable to matching pattern")
	public void pattern1() {
		assertTrue(patternType(regexpType("[abc]+")).isAssignable(stringType("abc")));
	}

	@Test
	@DisplayName("can be used as a Hash key")
	public void asHashKey() {
		Map<AnyType, AnyType> typesByType = new HashMap<>();
		for(AnyType type : TypeEvaluatorImpl.BASIC_TYPES.values())
			typesByType.put(type, type);
		assertTrue(all(TypeEvaluatorImpl.BASIC_TYPES.values(), type -> typesByType.get(type) == type));
	}
}
