package com.puppet.pcore.parser.model;

public class LiteralUndef extends LiteralExpression {
	public LiteralUndef(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}

	@Override
	public Object value() {
		return null;
	}
}
