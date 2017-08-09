package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class VirtualQuery extends QueryExpression {
	public VirtualQuery(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}
}
