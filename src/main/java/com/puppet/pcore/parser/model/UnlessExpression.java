package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class UnlessExpression extends IfExpression {
	public UnlessExpression(Expression test, Expression then, Expression elseExpr, Locator locator, int offset, int length) {
		super(test, then, elseExpr, locator, offset, length);
	}
}
