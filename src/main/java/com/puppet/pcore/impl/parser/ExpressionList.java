package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static java.util.Collections.unmodifiableList;

abstract class ExpressionList extends AbstractExpression {
	public final List<Expression> elements;

	ExpressionList(List<Expression> parameters, String expression, int offset, int length) {
		super(expression, offset, length);
		this.elements = unmodifiableList(parameters);
	}

	public boolean equals(Object o) {
		return o != null && getClass().equals(o.getClass()) && elements.equals(((ExpressionList)o).elements);
	}

	public int hashCode() {
		return getClass().hashCode() * 31 + elements.hashCode();
	}
}
