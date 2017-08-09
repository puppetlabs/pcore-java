package com.puppet.pcore.parser.model;

import com.puppet.pcore.Default;

public class LiteralDefault extends LiteralExpression {
	public LiteralDefault(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}

	@Override
	public Object value() {
		return Default.SINGLETON;
	}
}
