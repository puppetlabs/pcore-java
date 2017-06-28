package com.puppet.pcore.impl.serialization;

public interface Converter {
	String PCORE_TYPE_KEY = "__pcore_type__";

  /** Key used when the value can be represented as, and recreated from, a single string that can
   * be passed to a `from_string` method or an array of values that can be passed to the default
   * initializer method.
   */
  String PCORE_VALUE_KEY = "__pcore_value__";

	/**
	 * Type key used for hashes that contain keys that are not of type String
	 */
	String PCORE_TYPE_HASH = "Hash";

	/**
	 * Type key used for Sensitive
	 */
	String PCORE_TYPE_SENSITIVE = "Sensitive";

	/**
	 * Type key used for Symbol
	 */
	String PCORE_TYPE_SYMBOL = "Symbol";

	/**
	 * Type key used for 'Default'
	 */
	String PCORE_TYPE_DEFAULT = "Default";

	/**
	 * Type key used for document local references
	 */
	String PCORE_LOCAL_REF_SYMBOL = "LocalRef";
}
