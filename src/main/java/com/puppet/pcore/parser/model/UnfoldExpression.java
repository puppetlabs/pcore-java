package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class UnfoldExpression extends UnaryExpression {
	public UnfoldExpression(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}
}
