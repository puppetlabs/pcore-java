package com.puppet.pcore.impl.parser;

public class AbstractExpression implements com.puppet.pcore.parser.Expression {
	final String expression;
	final int length;
	final int offset;

	AbstractExpression(String expression, int offset, int length) {
		this.expression = expression;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public String toString() {
		return expression.substring(offset, offset + length);
	}
}
