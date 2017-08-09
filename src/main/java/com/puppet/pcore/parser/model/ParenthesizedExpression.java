package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class ParenthesizedExpression extends UnaryExpression {
	public ParenthesizedExpression(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}
}
