package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

import java.util.List;

public class ResourceTypeDefinition extends NamedDefinition {
	public ResourceTypeDefinition(String name, List<Parameter> parameters, Expression body, Locator locator, int offset, int length) {
		super(name, parameters, body, locator, offset, length);
	}

	@Override
	public PN toPN() {
		return definitionPN("define", null, null);
	}
}
