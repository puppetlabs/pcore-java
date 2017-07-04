package com.puppet.pcore.serialization;

import com.puppet.pcore.PcoreException;
import com.puppet.pcore.Type;

import java.io.IOException;
import java.util.List;

/**
 * Provides access to positional arguments. An instance of this interface is typically
 * passed to a {@link FactoryFunction} to create an instance of an object.
 */
public interface ArgumentsAccessor {
	/**
	 * Returns the argument at the given _index_
	 * @param index the zero based index of the argument
	 * @return the argument at the given _index_
	 * @throws IOException if a {@link Deserializer} was unable to read the argument
	 */
	Object get(int index) throws IOException;

	/**
	 * @return all arguments as an array
	 * @throws IOException if a {@link Deserializer} was unable to read the arguments
	 */
	Object[] getAll() throws IOException;


	/**
	 * @return all arguments as a list
	 * @throws IOException if a {@link Deserializer} was unable to read the arguments
	 */
	List<Object> getArgumentList() throws IOException;

	/**
	 * Returns the type that describes the parameters tuple
	 * @return the parameters type
	 */
	public Type getParametersType();

	/**
	 * The type that the arguments are intended for
	 * @return the argument type
	 */
	Type getType();

	/**
	 * Tells the caller to remember an instance that is created. Objects that may contain self
	 * references must call this method before retrieving any arguments.
	 *
	 * @param createdInstance the instance that is created
	 * @param <T> the type of the instance
	 * @return the argument
	 */
	<T> T remember(T createdInstance);

	/**
	 * @return the number of arguments that this instance can provide
	 */
	int size();
}
