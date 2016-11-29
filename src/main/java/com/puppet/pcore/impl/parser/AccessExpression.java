package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class AccessExpression extends ExpressionList {
	public final Expression expr;

	public AccessExpression(Expression expr, List<Expression> parameters, String expression, int offset, int length) {
		super(parameters, expression, offset, length);
		this.expr = expr;
	}

	public boolean equals(Object o) {
		return super.equals(o) && expr.equals(((AccessExpression)o).expr);
	}

	public int hashCode() {
		return super.hashCode() * 31 + expr.hashCode();
	}
}
