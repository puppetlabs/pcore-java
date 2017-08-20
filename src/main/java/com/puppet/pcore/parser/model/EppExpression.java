package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

public class EppExpression extends Positioned {
	public final boolean parametersSpecified;

	public final Expression body;

	public EppExpression(boolean parametersSpecified, Expression body, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.parametersSpecified = parametersSpecified;
		this.body = body;
	}

	public boolean equals(Object o) {
		return super.equals(o) && parametersSpecified == ((EppExpression)o).parametersSpecified && body.equals(((EppExpression)o).body);
	}

	@Override
	public PN toPN() {
		return body.toPN().asCall("epp");
	}
}
