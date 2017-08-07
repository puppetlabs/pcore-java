package com.puppet.pcore.serialization;

import java.util.List;

/**
 * Represents the constructor for a type.
 * @param <T>
 */
@FunctionalInterface
public interface FactoryFunction<T> {
	T createInstance(List<?> args);
}
