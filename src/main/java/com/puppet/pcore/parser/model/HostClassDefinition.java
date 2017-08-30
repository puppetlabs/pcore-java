package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

public class HostClassDefinition extends NamedDefinition {
	public final String parentClass;

	public HostClassDefinition(String name, String parentClass, List<Parameter> parameters, Expression body, Locator locator, int offset, int length) {
		super(name, parameters, body, locator, offset, length);
		this.parentClass = parentClass;
	}

	public boolean equals(Object o) {
		return super.equals(o) && Objects.equals(parentClass, ((HostClassDefinition)o).parentClass);
	}

	@Override
	public PN toPN() {
		return definitionPN("class", parentClass, null);
	}
}
