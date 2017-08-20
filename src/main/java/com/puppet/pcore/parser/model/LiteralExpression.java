package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;

public abstract class LiteralExpression extends Positioned {

	public LiteralExpression(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}

	@Override
	public PN toPN() {
		return new LiteralPN(value());
	}

	public abstract Object value();
}

