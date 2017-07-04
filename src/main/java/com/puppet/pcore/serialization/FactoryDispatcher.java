package com.puppet.pcore.serialization;

import com.puppet.pcore.Type;

import java.io.IOException;
import java.util.List;

/**
 * Represents all constructors for a type.
 * @param <C> implementation class
 */
public interface FactoryDispatcher<C> {
	List<? extends Constructor<C>> constructors();

	C createInstance(Type type, ArgumentsAccessor aa) throws IOException;

	C createInstance(Type type, List<Object> args);

	C createInstance(Type type, Object... args);
}
