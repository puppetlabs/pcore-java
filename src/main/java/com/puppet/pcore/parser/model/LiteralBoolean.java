package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class LiteralBoolean extends LiteralExpression {
	public final boolean value;

	public LiteralBoolean(boolean value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.value = value;
	}

	public boolean equals(Object o) {
		return super.equals(o) && value == ((LiteralBoolean)o).value;
	}

	@Override
	public Object value() {
		return value;
	}
}
