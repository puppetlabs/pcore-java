package com.puppet.pcore.serialization;

import com.puppet.pcore.Type;

/**
 * Represents the constructor for a type.
 * @param <T> implementation class
 */
public interface Constructor<T> {
	FactoryFunction<T> initFunction();

	Type signature();

	boolean isHashConstructor();
}
