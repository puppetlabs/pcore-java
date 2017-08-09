package com.puppet.pcore.parser.model;

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
}
