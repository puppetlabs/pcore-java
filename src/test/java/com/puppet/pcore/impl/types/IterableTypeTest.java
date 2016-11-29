package com.puppet.pcore.impl.types;

import com.puppet.pcore.impl.TypeEvaluatorImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.types.TypeFactory.iterableType;
import static com.puppet.pcore.impl.types.TypeFactory.stringType;
import static java.util.stream.Collectors.partitioningBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("Iterable Type")
public class IterableTypeTest {
	@Test
	@DisplayName("responds true to isIterable()")
	public void test1() {
		Assertions.assertTrue(iterableType().isIterable(null));
	}

	@Test
	@DisplayName("returns Iterable with generalized element type when generalized")
	public void test2() {
		Assertions.assertEquals(iterableType(stringType(0, 10)).generalize(), iterableType(stringType()));
	}

	@Test
	@DisplayName("considers Iterable with assignable element type as assignable")
	public void test3() {
		Assertions.assertTrue(iterableType(stringType()).isAssignable(iterableType
				(stringType(1, 10))));
	}

	@Test
	@DisplayName("considers Iterable with element type as assignable to default")
	public void test4() {
		Assertions.assertTrue(iterableType().isAssignable(iterableType(stringType(1, 10))));
	}

	@Nested
	@DisplayName("when asked for an Iterable type")
	class AskedForIterable {
		@Test
		@DisplayName("the default iterator type returns the default iterable type")
		public void test1() {
			Assertions.assertEquals(iterableType().asIterableType(null), iterableType());
		}

		@Test
		@DisplayName("a typed iterator type returns the an equally typed iterable type")
		public void test2() {
			Assertions.assertEquals(iterableType(stringType()).asIterableType(null), iterableType(stringType()));
		}

		@Test
		@DisplayName("iterable types will produce an iterable")
		public void test3() {
			Map<Boolean,List<AnyType>> partitioned = TypeEvaluatorImpl.BASIC_TYPES.values().stream().collect(partitioningBy(AnyType::isIterable));
			assertTrue(partitioned.get(true).stream().allMatch(type -> type.asIterableType() != null));
			assertTrue(partitioned.get(false).stream().allMatch(type -> type.asIterableType() == null));
		}
	}
}
