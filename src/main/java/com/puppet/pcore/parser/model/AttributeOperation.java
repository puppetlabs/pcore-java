package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class AttributeOperation extends Positioned {
	public final String operator;
	public final String name;
	public final Expression value;

	public AttributeOperation(String operator, String name, Expression value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.operator = operator;
		this.name = name;
		this.value = value;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		AttributeOperation co = (AttributeOperation)o;
		return operator.equals(co.operator) && name.equals(co.name) && value.equals(co.value);
	}
}
