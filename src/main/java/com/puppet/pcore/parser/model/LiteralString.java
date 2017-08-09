package com.puppet.pcore.parser.model;

public class LiteralString extends LiteralExpression implements NameExpression {
	public final String value;

	public LiteralString(String value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.value = value;
	}

	public boolean equals(Object o) {
		return super.equals(o) && value.equals(((LiteralString)o).value);
	}

	@Override
	public String name() {
		return value;
	}

	@Override
	public Object value() {
		return value;
	}
}
