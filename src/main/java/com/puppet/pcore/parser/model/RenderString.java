package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;

public class RenderString extends LiteralString {
	public RenderString(String value, Locator locator, int offset, int length) {
		super(value, locator, offset, length);
	}

	@Override
	public PN toPN() {
		return new LiteralPN(value).asCall("render-s");
	}
}
