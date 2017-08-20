package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.parser.Expression;

public class VariableExpression extends UnaryExpression {
	public VariableExpression(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}

	@Override
	public PN toPN() {
		return new CallPN("$", new LiteralPN(((QualifiedName)expr).name));
	}
}
