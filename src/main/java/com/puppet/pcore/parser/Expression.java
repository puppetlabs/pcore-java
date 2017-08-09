package com.puppet.pcore.parser;

/**
 * Represents an expression parsed by the {@link ExpressionParser}
 */
public interface Expression {
	/**
	 * Offset in source where this expression starts
	 * @return the source offset
	 */
	int offset();

	/**
	 * Length of this expressions representation in source
	 * @return the source length
	 */
	int length();
}
