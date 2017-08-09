package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.Objects;

public class Parameter extends Positioned {
	public final String name;

	public final Expression value;

	public final Expression type;

	public final boolean capturesRest;

	public Parameter(String name, Expression type, Expression value, boolean capturesRest, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.name = name;
		this.type = type;
		this.value = value;
		this.capturesRest = capturesRest;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		Parameter co = (Parameter)o;
		return capturesRest == co.capturesRest && name.equals(co.name) && Objects.equals(type, co.type) && Objects.equals(value, co.value);
	}
}
