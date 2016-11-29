package com.puppet.pcore.loader;

import com.puppet.pcore.NoSuchTypeException;
import com.puppet.pcore.TypeRedefinedException;

import java.net.URI;

public interface Loader {
	/**
	 * Bind an object to a {@link TypedName}. Throws an exception if this loader or a loader
	 * in the parent chain of loaders already has an object bound by the same name.
	 *
	 * @param name the name to use for the binding
	 * @param toBeBound the object to bind
	 * @throws TypeRedefinedException if an object is already bound by the same name.
	 */
	void bind(TypedName name, Object toBeBound) throws TypeRedefinedException;

	/**
	 * @return the default name authority for this loader.
	 */
	URI getNameAuthority();

	/**
	 * Lookup and load and return the object that maps to the given name. The lookup
	 * will first search the parent chain and only consult this loader if no object
	 * was found there.
	 *
	 * @param name the name to use for the lookup
	 * @return the loaded object
	 * @throws NoSuchTypeException if no object could be found for the given name
	 */
	Object load(TypedName name) throws NoSuchTypeException;

	/**
	 * Lookup and load and return the object that maps to the given name. The lookup
	 * will first search the parent chain and only consult this loader if no object
	 * was found there.
	 *
	 * @param name the name to use for the lookup
	 * @return the loaded object or {@code null} if no such object could be found
	 */
	Object loadOrNull(TypedName name);
}
