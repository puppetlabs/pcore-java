package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.*;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.map;
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

	PN definitionPN(String typeName, String parent, Expression returnType) {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(new LiteralPN(name).withName("name"));
		if(parent != null)
			entries.add(new LiteralPN(parent).withName("parent"));
		if(!parameters.isEmpty())
			entries.add(new ListPN(map(parameters, Expression::toPN)).withName("params"));
		if(body != null)
			entries.add(body.toPN().withName("body"));
		if(returnType != null)
			entries.add(returnType.toPN().withName("returns"));

		return new MapPN(entries).asCall(typeName);
	}
}
