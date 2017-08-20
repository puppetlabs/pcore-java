package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

public class AndExpression extends BooleanExpression {
	public AndExpression(Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(lhs, rhs, locator, offset, length);
	}

	@Override
	public PN toPN() {
		return binaryPN("and");
	}
}
