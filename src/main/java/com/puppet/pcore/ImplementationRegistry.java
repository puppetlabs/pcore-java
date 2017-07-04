package com.puppet.pcore;

import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.serialization.FactoryFunction;

import java.util.function.Function;

/**
 * The <code>ImplementationRegistry</code> maps names types in the Puppet Type System to names of corresponding
 * implementation classes. Each mapping is unique and bidirectional so that for any given type name there is only one
 * implementation and vice versa.
 */
public interface ImplementationRegistry {

	/**
	 * Find the ObjectWriter for the given implementation class.
	 *
	 * @param type the implementation class
	 * @return the ObjectWriter, or <code>null</code> if not found.
	 */
	<T> Function<T,Object[]> attributeProviderFor(Type type);

	/**
	 * Find the {@link FactoryFunction} for the given implementation class.
	 *
	 * @param type the type
	 * @return the ObjectReader, or <code>null</code> if not found.
	 */
	<T> FactoryDispatcher<T> creatorFor(Type type);

	/**
	 * Register a bidirectional mapping between a type and an implementation
	 *
	 * @param type    the Pcore type
	 * @param creator the factory dispatcher used when creating instances of the type
	 * @param attributeSupplier attribute supplier capable of extractin attributes from an instance of the type
	 */
	<T> void registerImplementation(Type type, FactoryDispatcher<T> creator, Function<T,Object[]> attributeSupplier);

	/**
	 * Register a bidirectional mapping between a type and an implementation
	 *
	 * @param typeName the name of the type
	 * @param creator the factory dispatcher used when creating instances of the type
	 * @param attributeSupplier attribute supplier capable of extractin attributes from an instance of the type
	 */
	<T> void registerImplementation(String typeName, FactoryDispatcher<T> creator, Function<T,Object[]>
			attributeSupplier);

	/**
	 * Register a bidirectional namespace mapping
	 *
	 * @param typeNamespace the namespace for the puppet types
	 * @param implNamespace the namespace for the implementations
	 */
	void registerNamespace(String typeNamespace, String implNamespace);

	/**
	 * Register a bidirectional pattern mapping
	 *
	 * @param typeNameSubst pattern and replacement mapping type names to runtime names
	 * @param implNameSubst pattern and replacement mapping runtime names to type names
	 */
	void registerPatternMapping(PatternSubstitution typeNameSubst, PatternSubstitution implNameSubst);

	/**
	 * Register a bidirectional type mapping.
	 *
	 * @param runtimeType  type containing the pattern and replacement to map the runtime type to a puppet type
	 * @param substitution the pattern and replacement to map a puppet type to a runtime type
	 */
	void registerTypeMapping(Type runtimeType, PatternSubstitution substitution);
}
