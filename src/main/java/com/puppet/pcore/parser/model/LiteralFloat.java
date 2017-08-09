package com.puppet.pcore.parser.model;

public class LiteralFloat extends LiteralExpression {
	public final double value;

	public LiteralFloat(double value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.value = value;
	}

	public boolean equals(Object o) {
		return super.equals(o) && value == ((LiteralFloat)o).value;
	}

	@Override
	public Object value() {
		return value;
	}
}
