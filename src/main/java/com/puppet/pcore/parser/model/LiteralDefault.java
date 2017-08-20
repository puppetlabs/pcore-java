package com.puppet.pcore.parser.model;

import com.puppet.pcore.Default;
import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;

public class LiteralDefault extends LiteralExpression {
	public LiteralDefault(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}

	@Override
	public Object value() {
		return Default.SINGLETON;
	}

	@Override
	public PN toPN() {
		return new CallPN("default");
	}
}
