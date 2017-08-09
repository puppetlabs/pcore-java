package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class BlockExpression extends Positioned {
	public final List<Expression> statements;

	public BlockExpression(List<Expression> statements, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.statements = unmodifiableCopy(statements);
	}

	public boolean equals(Object o) {
		return super.equals(o) && statements.equals(((BlockExpression)o).statements);
	}
}
