package com.puppet.pcore.impl.parser;

import java.util.Objects;

public class HeredocExpression extends ConstantExpression {
	public final String syntax;

	public HeredocExpression(Object value, String syntax, String expression, int offset, int length) {
		super(value, expression, offset, length);
		this.syntax = syntax;
	}

	public boolean equals(Object o) {
		return super.equals(o) && Objects.equals(syntax, ((HeredocExpression)o).syntax);
	}

	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(syntax);
	}
}
