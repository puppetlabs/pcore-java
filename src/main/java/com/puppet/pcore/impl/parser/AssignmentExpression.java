package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

public class AssignmentExpression extends BinaryExpression {

	public AssignmentExpression(Expression lhs, Expression rhs, String expression, int offset, int length) {
		super(lhs, rhs, expression, offset, length);
	}
}
