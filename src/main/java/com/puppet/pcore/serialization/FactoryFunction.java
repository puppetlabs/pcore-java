package com.puppet.pcore.serialization;

import java.io.IOException;

/**
 * Represents the constructor for a type.
 * @param <T>
 */
@FunctionalInterface
public interface FactoryFunction<T> {
	T createInstance(ArgumentsAccessor attrs) throws IOException;
}
