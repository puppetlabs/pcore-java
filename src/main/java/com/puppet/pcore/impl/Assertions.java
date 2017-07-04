package com.puppet.pcore.impl;

import com.puppet.pcore.TypeAssertionException;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.impl.types.IntegerType;

import java.util.function.Supplier;

import static com.puppet.pcore.impl.types.TypeFactory.inferSet;
import static java.lang.String.format;

public class Assertions {
	public static void assertMinMax(double min, double max, Supplier<String> identifier) {
		if(min > max)
			throw new TypeAssertionException(format("%s min must be <= max", identifier.get()));
	}

	public static <T extends Comparable<T>> void assertMinMax(T min, T max, Supplier<String> identifier) {
		if(min.compareTo(max) > 0)
			throw new TypeAssertionException(format("%s min must be <= max", identifier.get()));
	}

	public static void assertMinMax(long min, long max, Supplier<String> identifier) {
		if(min > max)
			throw new TypeAssertionException(format("%s min must be <= max", identifier.get()));
	}

	public static <T> T assertNotNull(T value, Supplier<String> identifier) {
		if(value == null)
			throw new TypeAssertionException(format("%s can not be null", identifier.get()));
		return value;
	}

	public static IntegerType assertPositive(IntegerType type, Supplier<String> identifier) {
		assertNotNull(type, identifier);
		assertPositive(type.min, () -> format("%s.min", identifier.get()));
		assertPositive(type.max, () -> format("%s.max", identifier.get()));
		return type;
	}

	public static long assertPositive(long value, Supplier<String> identifier) {
		if(value < 0)
			throw new TypeAssertionException(format("%s must be >= 0", identifier.get()));
		return value;
	}

	public static AnyType assertType(AnyType expected, Object actual, Supplier<String> identifier) {
		if(!(actual instanceof AnyType))
			throw new TypeAssertionException(format("%s is not a Type", identifier.get()));

		AnyType at = (AnyType)actual;
		if(!expected.isAssignable(at))
			throw new TypeAssertionException(identifier.get() + ' ' + TypeMismatchDescriber.SINGLETON.describeMismatch(expected, at));
		return at;
	}

	public static Object assertInstance(AnyType expected, Object actual, Supplier<String> identifier) {
		if(!expected.isInstance(actual))
			throw new TypeAssertionException(identifier.get() + ' ' + TypeMismatchDescriber.SINGLETON.describeMismatch(expected, inferSet(actual)));
		return actual;
	}
}
