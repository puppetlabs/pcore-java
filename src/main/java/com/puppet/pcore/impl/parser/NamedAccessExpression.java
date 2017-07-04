package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class NamedAccessExpression extends BinaryExpression {
	public NamedAccessExpression(Expression lhs, Expression rhs, String expression, int offset, int length) {
		super(lhs, rhs, expression, offset, length);
	}
}
