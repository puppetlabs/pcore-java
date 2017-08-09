package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public abstract class BooleanExpression extends BinaryExpression {
	public BooleanExpression(Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(lhs, rhs, locator, offset, length);
	}
}
