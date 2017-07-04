package com.puppet.pcore.impl.parser;

public class VariableExpression extends IdentifierExpression {
	public final String name;

	public VariableExpression(String name, String expression, int offset, int length) {
		super(name, expression, offset, length);
		this.name = name;
	}
}
