package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.Objects;

public class TypeDefinition extends QRefDefinition {
	public final String parent;

	public final Expression body;

	public TypeDefinition(String name, String parent, Expression body, Locator locator, int offset, int length) {
		super(name, locator, offset, length);
		this.parent = parent;
		this.body = body;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		TypeDefinition co = (TypeDefinition)o;
		return Objects.equals(parent, co.parent) && body.equals(co.body);
	}
}
