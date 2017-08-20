package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

public class AssignmentExpression extends BinaryExpression {
	public final String operator;

	public AssignmentExpression(String operator, Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(lhs, rhs, locator, offset, length);
		this.operator = operator;
	}

	public boolean equals(Object o) {
		return super.equals(o) && operator.equals(((AssignmentExpression)o).operator);
	}

	@Override
	public PN toPN() {
		return binaryPN(operator);
	}
}
