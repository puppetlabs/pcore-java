package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.Objects;

public class TypeDeclarationExpression extends IdentifierExpression {
	public final String name;

	public TypeDeclarationExpression(String name, String expression, int offset, int length) {
		super(name, expression, offset, length);
		this.name = name;
	}
}
