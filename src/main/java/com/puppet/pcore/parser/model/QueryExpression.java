package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public abstract class QueryExpression extends Positioned {
	public final Expression expr;

	public QueryExpression(Expression expr, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.expr = expr;
	}

	public boolean equals(Object o) {
		return super.equals(o) && expr.equals(((QueryExpression)o).expr);
	}
}
