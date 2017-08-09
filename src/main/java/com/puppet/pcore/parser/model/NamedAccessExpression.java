package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class NamedAccessExpression extends BinaryExpression {
	public NamedAccessExpression(Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(lhs, rhs, locator, offset, length);
	}
}
