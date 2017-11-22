package com.puppet.pcore;

import com.puppet.pcore.serialization.FactoryDispatcher;

/**
 * A Pcore type.
 */
public interface Type {
	/**
	 * Answers, 'What is the common type of this type and _other_?'
	 * @param other the other type
	 * @return the common type
	 */
	Type common(Type other);

	/**
	 * Returns the generalization of this type.
	 * @return the generalization of this type
	 */
	Type generalize();

	/**
	 * Returns the constructor parameter combinations that can be used when creating
	 * instances of this type.
	 *
	 * @return list of tuples where each tuple describes the parameter signature of a constructor
	 * @throws PcoreException if creating instances of this type is an unsupported operatoin
	 */
	<T> FactoryDispatcher<T> factoryDispatcher();

	/**
	 * Checks if the given type is assignable to this type
	 * @param t the type to check
	 * @return {@code true} if the _t_ is assignable to this type
	 */
	boolean isAssignable(Type t);


	/**
	 * Checks if the given value is an instance of this type
	 * @param o the value to check
	 * @return {@code true} if the _o_ is an instance of this type
	 */
	boolean isInstance(Object o);

	/**
	 * @return the name of this type
	 */
	String name();

	/**
	 * Creates a new instance of the type.
	 * Not all types can produce instances.
	 * @param args arguments to use when creating the instance
	 * @return the created instance
	 * @throws PcoreException if the type is unable to create an instance
	 */
	Object newInstance(Object...args);

	/**
	 * Normalizes the type. This does not change the characteristics of the type but it will remove duplicates
	 * and constructs like NotUndef[T] where T is not assignable from Undef and change Variant[*T] where all
	 * T are enums into an Enum.
	 *
	 * @return the normalized type or _this_ if the type was already normalized
	 */
	Type normalize();

	String toDebugString();

	String toExpandedString();
}
