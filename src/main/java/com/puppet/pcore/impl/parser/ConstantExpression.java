package com.puppet.pcore.impl.parser;

import java.util.Objects;

public class ConstantExpression extends AbstractExpression implements StringExpression {
	public final Object value;

	public ConstantExpression(Object value, String expression, int offset, int length) {
		super(expression, offset, length);
		this.value = value;
	}

	public boolean equals(Object o) {
		return o != null && getClass().equals(o.getClass()) && Objects.equals(value, ((ConstantExpression)o).value);
	}

	@Override
	public String getString() {
		return value instanceof String ? (String)value : null;
	}

	public int hashCode() {
		return getClass().hashCode() * 31 + Objects.hashCode(value);
	}
}
