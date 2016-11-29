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
}
