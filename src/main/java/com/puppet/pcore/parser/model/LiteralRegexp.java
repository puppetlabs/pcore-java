package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.regex.Regexp;

public class LiteralRegexp extends LiteralExpression {
	public final Regexp value;

	public LiteralRegexp(String value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.value = Regexp.compile(value);
	}

	public boolean equals(Object o) {
		return super.equals(o) && value.equals(((LiteralRegexp)o).value);
	}

	@Override
	public Object value() {
		return value;
	}

	@Override
	public PN toPN() {
		return new LiteralPN(value.toString()).asCall("regexp");
	}
}
