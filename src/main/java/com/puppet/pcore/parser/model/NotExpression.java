package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class NotExpression extends UnaryExpression {
	public NotExpression(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}
}
