package com.puppet.pcore.parser.model;

public class LiteralRegexp extends LiteralExpression {
	public final String value;

	public LiteralRegexp(String value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.value = value;
	}

	public boolean equals(Object o) {
		return super.equals(o) && value.equals(((LiteralRegexp)o).value);
	}

	@Override
	public Object value() {
		return value;
	}
}
