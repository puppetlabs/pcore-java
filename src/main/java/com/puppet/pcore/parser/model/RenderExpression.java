package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class RenderExpression extends UnaryExpression {
	public RenderExpression(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}
}
