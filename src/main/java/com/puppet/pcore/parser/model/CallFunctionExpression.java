package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class CallFunctionExpression extends CallExpression {
	CallFunctionExpression(Expression functor, List<Expression> arguments, Expression lambda, boolean rvalRequired,
			Locator locator, int offset, int length) {
		super(functor, arguments, lambda, rvalRequired, locator, offset, length);
	}
}
