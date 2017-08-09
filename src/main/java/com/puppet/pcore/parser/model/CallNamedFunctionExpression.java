package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class CallNamedFunctionExpression extends CallExpression {
	public CallNamedFunctionExpression(Expression functor, List<Expression> arguments, Expression lambda, boolean rvalRequired,
			Locator locator, int offset, int length) {
		super(functor, arguments, lambda, rvalRequired, locator, offset, length);
	}


	public CallNamedFunctionExpression withRvalRequired(boolean rvalRequired) {
		return this.rvalRequired == rvalRequired ? this : new CallNamedFunctionExpression(
				this.functor, this.arguments, this.lambda, rvalRequired, this.locator, this.offset, this.length);
	}
}
