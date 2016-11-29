package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class ArrayExpression extends ExpressionList {
	public ArrayExpression(List<Expression> parameters, String expression, int offset, int length) {
		super(parameters, expression, offset, length);
	}
}
