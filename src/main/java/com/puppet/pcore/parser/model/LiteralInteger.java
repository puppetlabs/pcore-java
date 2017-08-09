package com.puppet.pcore.parser.model;

public class LiteralInteger extends LiteralExpression {
	public final long value;

	public final int radix;

	public LiteralInteger(long value, int radix, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.value = value;
		this.radix = radix;
	}

	public boolean equals(Object o) {
		return super.equals(o) && value == ((LiteralInteger)o).value && radix == ((LiteralInteger)o).radix;
	}

	@Override
	public Object value() {
		return value;
	}
}
