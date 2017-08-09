package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class ResourceTypeDefinition extends NamedDefinition {
	public ResourceTypeDefinition(String name, List<Parameter> parameters, Expression body, Locator locator, int offset, int length) {
		super(name, parameters, body, locator, offset, length);
	}
}
