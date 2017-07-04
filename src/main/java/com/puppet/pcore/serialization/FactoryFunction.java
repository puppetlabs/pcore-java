package com.puppet.pcore.serialization;

import java.io.IOException;
import java.util.List;

/**
 * Represents the constructor for a type.
 * @param <T>
 */
@FunctionalInterface
public interface FactoryFunction<T> {
	T createInstance(List<? extends Object> args);
}
