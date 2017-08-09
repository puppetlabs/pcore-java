package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public abstract class NamedDefinition extends Definition {
	public final String name;

	public final List<Parameter> parameters;

	public final Expression body;

	public NamedDefinition(String name, List<Parameter> parameters, Expression body, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.name = name;
		this.parameters = unmodifiableCopy(parameters);
		this.body = body;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		NamedDefinition co = (NamedDefinition)o;
		return name.equals(co.name) && parameters.equals(co.parameters) && body.equals(co.body);
	}
}
