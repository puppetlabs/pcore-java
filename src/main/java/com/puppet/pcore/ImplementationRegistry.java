package com.puppet.pcore;

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
	 * @param implClass
	 * @return the ObjectWriter, or <code>null</code> if not found.
	 */
	<T> Function<T,Object[]> attributeProviderFor(Class<T> implClass);

	/**
	 * Find the implementation class for a given type
	 *
	 * @param type   the type
	 * @param loader the loader to use when loading the class
	 * @return the implementation class, or <code>null</code> if no class could be found for the given type.
	 */
	Class<?> classFor(Type type, ClassLoader loader);

	/**
	 * Find the implementation class for a given type
	 *
	 * @param typeName the name of the type
	 * @param loader   the loader to use when loading the class
	 * @return the implementation class, or <code>null</code> if no class could be found for the given type.
	 */
	Class<?> classFor(String typeName, ClassLoader loader);

	/**
	 * Find the {@link FactoryFunction} for the given implementation class.
	 *
	 * @param implClass
	 * @return the ObjectReader, or <code>null</code> if not found.
	 */
	<T> FactoryFunction<T> creatorFor(Class<T> implClass);

	/**
	 * Register a bidirectional mapping between a type and an implementation
	 *
	 * @param type      the Pcore type
	 * @param implClass the implementation class
	 */
	<T> void registerImplementation(
			Type type, Class<T> implClass, FactoryFunction<T> creator, Function<T,Object[]>
			attributeSupplier);

	/**
	 * Register a bidirectional mapping between a type and an implementation
	 *
	 * @param typeName the name of the type
	 * @param implName the name of the implementation class
	 */
	<T> void registerImplementation(
			String typeName, String implName, FactoryFunction<T> creator, Function<T,Object[]>
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
	 * @param runtimeType type that represents the runtime class to map to a puppet type
	 * @param puppetType  type that will be mapped to the runtime class
	 */
	<T> void registerTypeMapping(
			Type runtimeType, Type puppetType, FactoryFunction<T> creator, Function<T,Object[]>
			attributeSupplier);

	/**
	 * Register a bidirectional type mapping.
	 *
	 * @param runtimeType  type containing the pattern and replacement to map the runtime type to a puppet type
	 * @param substitution the pattern and replacement to map a puppet type to a runtime type
	 */
	void registerTypeMapping(Type runtimeType, PatternSubstitution substitution);

	/**
	 * Find the type for a given implementation class
	 *
	 * @param implClass the implementation class
	 * @param evaluator the evaluator to use when evaluating the type expression
	 * @return the type, or <code>null</code> if no type could be found for the given class.
	 */
	Type typeFor(Class<?> implClass, TypeEvaluator evaluator);

	/**
	 * Find the type for a given implementation class
	 *
	 * @param implClassName the name of the implementation class
	 * @param evaluator     the evaluator to use when evaluating the type expression
	 * @return the type, or <code>null</code> if no type could be found for the given class name.
	 */
	Type typeFor(String implClassName, TypeEvaluator evaluator);
}
