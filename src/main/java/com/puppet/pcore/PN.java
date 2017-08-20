package com.puppet.pcore;

import java.util.Map.Entry;

public interface PN {
	/**
	 * Produces a compact, LISP like syntax. Suitable for tests.
	 * @param bld the receiver of the produced output
	 */
	void format(StringBuilder bld);

	/**
	 * Produces an object that where all values are of primitive type, lists,
	 * or maps. This format is suitable for output as JSON or YAML
	 * @return the Data object
	 */
	Object toData();

	/**
	 * Turn this PN into parameters for a named call, or change the name if it this
	 * already is a named call.
	 * @param name the name of the call
	 * @return the named entry
	 */
	PN asCall(String name);

	/**
	 * Create a named PN. If the PN is already named, a new Named PN with the same
	 * contents will be created.
	 * @param name the name of the entry
	 * @return the named entry
	 */
	Entry<String, PN> withName(String name);
}

