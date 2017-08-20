package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.parser.Expression;

public abstract class BinaryExpression extends Positioned {
	public final Expression lhs;

	public final Expression rhs;

	public BinaryExpression(Expression lhs, Expression rhs, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		BinaryExpression co = (BinaryExpression)o;
		return lhs.equals(co.lhs) && rhs.equals(co.rhs);
	}

	PN binaryPN(String name) {
		return new CallPN(name, lhs.toPN(), rhs.toPN());
	}
}
