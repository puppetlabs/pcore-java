package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

abstract class BinaryExpression extends AbstractExpression {
	public final Expression lhs;
	public final Expression rhs;

	public BinaryExpression(Expression lhs, Expression rhs, String expression, int offset, int length) {
		super(expression, offset, length);
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public boolean equals(Object o) {
		if(o != null && getClass().equals(o.getClass())) {
			BinaryExpression be = (BinaryExpression)o;
			return lhs.equals(be.lhs) && rhs.equals(be.rhs);
		}
		return false;
	}

	public int hashCode() {
		return lhs.hashCode() * 31 + rhs.hashCode();
	}
}
