package com.puppet.pcore.parser;

import com.puppet.pcore.PN;

/**
 * Represents an expression parsed by the {@link ExpressionParser}
 */
public interface Expression {
	/**
	 * @return offset in source where this expression starts
	 */
	int offset();

	/**
	 * @return length of this expressions representation in source
	 */
	int length();

	/**
	 * @return the PN representation of the expression
	 */
	PN toPN();
}
