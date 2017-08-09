package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class MatchExpression extends BinaryExpression {
	public final String operator;

	public MatchExpression(String operator, Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(lhs, rhs, locator, offset, length);
		this.operator = operator;
	}

	public boolean equals(Object o) {
		return super.equals(o) && operator.equals(((MatchExpression)o).operator);
	}
}
