package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.parser.Expression;

public class TypeAlias extends QRefDefinition {
	public final Expression type;

	public TypeAlias(String name, Expression type, Locator locator, int offset, int length) {
		super(name, locator, offset, length);
		this.type = type;
	}

	public boolean equals(Object o) {
		return super.equals(o) && type.equals(((TypeAlias)o).type);
	}

	@Override
	public PN toPN() {
		return new CallPN("type-alias", new LiteralPN(name), type.toPN());
	}
}
