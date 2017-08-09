package com.puppet.pcore.parser.model;

public abstract class LiteralExpression extends Positioned {

	public LiteralExpression(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}

	public abstract Object value();
}

