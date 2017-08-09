package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class VariableExpression extends UnaryExpression {
	public VariableExpression(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}
}
