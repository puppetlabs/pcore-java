package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;

public class NopExpression extends Positioned {
	public NopExpression(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}

	@Override
	public PN toPN() {
		return new CallPN("nop");
	}
}
