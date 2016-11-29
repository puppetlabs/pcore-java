package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

abstract class UnaryExpression extends AbstractExpression {
	public final Expression expr;

	public UnaryExpression(Expression expr, String expression, int offset, int length) {
		super(expression, offset, length);
		this.expr = expr;
	}

	public boolean equals(Object o) {
		return o != null && getClass().equals(o.getClass()) && expr.equals(((UnaryExpression)o).expr);
	}

	public int hashCode() {
		return getClass().hashCode() * 31 + expr.hashCode();
	}
}
