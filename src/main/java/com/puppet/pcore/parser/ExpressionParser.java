package com.puppet.pcore.parser;

/**
 * A Parser for Pcore type expressions
 */
public interface ExpressionParser {
	/**
	 * Parser an expression in String form to an abstract syntax tree
	 * @param exprString the expression to parse
	 * @return the syntax tree
	 */
	Expression parse(String exprString);

	/**
	 * Parser an expression in String form to an abstract syntax tree
	 * @param sourceName the name of the source (used in warnings and errors)
	 * @param exprString the expression to parse
	 * @return the syntax tree
	 */
	Expression parse(String sourceName, String exprString);

	/**
	 * Parser an expression in String form to an abstract syntax tree
	 * @param sourceName the name of the source (used in warnings and errors)
	 * @param exprString the expression to parse
	 * @param eppMode if source is text with embedded puppet constructs
	 * @param singleExpression multiple expressions not allowed
	 * @return the syntax tree
	 */
	Expression parse(String sourceName, String exprString, boolean eppMode, boolean singleExpression);
}
