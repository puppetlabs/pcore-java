package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

public class FunctionDefinition extends NamedDefinition {
	public final Expression returnType;

	public FunctionDefinition(String name, List<Parameter> parameters, Expression body, Expression returnType, Locator locator, int offset, int length) {
		super(name, parameters, body, locator, offset, length);
		this.returnType = returnType;
	}

	public boolean equals(Object o) {
		return super.equals(o) && Objects.equals(returnType, ((FunctionDefinition)o).returnType);
	}

	@Override
	public PN toPN() {
		return definitionPN("function", null, returnType);
	}
}
