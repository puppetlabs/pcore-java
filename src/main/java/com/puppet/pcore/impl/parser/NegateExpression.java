package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

public class NegateExpression extends UnaryExpression {
	public NegateExpression(Expression expr, String expression, int offset, int length) {
		super(expr, expression, offset, length);
	}
}
