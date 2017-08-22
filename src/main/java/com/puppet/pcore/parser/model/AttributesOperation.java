package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

public class AttributesOperation extends Positioned {
	public final Expression expr;

	public AttributesOperation(Expression expr, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.expr = expr;
	}

	public boolean equals(Object o) {
		return super.equals(o) && expr.equals(((AttributesOperation)o).expr);
	}

	@Override
	public PN toPN() {
		return expr.toPN().asCall("splat_hash");
	}
}
