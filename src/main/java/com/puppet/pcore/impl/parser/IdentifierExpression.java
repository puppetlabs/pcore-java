package com.puppet.pcore.impl.parser;

public class IdentifierExpression extends AbstractExpression implements StringExpression {
	public final String name;

	public IdentifierExpression(String name, String expression, int offset, int length) {
		super(expression, offset, length);
		this.name = name;
	}

	public boolean equals(Object o) {
		return o != null && getClass().equals(o.getClass()) && name.equals(((IdentifierExpression)o).name);
	}

	@Override
	public String getString() {
		return name;
	}

	public int hashCode() {
		return getClass().hashCode() * 31 + name.hashCode();
	}

}
