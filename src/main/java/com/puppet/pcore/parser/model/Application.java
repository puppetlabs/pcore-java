package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class Application extends NamedDefinition {
	public Application(String name, List<Parameter> parameters, Expression body, Locator locator, int offset, int length) {
		super(name, parameters, body, locator, offset, length);
	}
}
