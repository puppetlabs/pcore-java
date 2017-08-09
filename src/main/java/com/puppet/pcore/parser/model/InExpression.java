package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class InExpression extends BinaryExpression {
	public InExpression(Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(lhs, rhs, locator, offset, length);
	}
}
