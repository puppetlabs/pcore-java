package com.puppet.pcore.impl.parser;

import java.util.Locale;

public class TypeNameExpression extends IdentifierExpression {
	private String downcasedName;

	public TypeNameExpression(String name, String expression, int offset, int length) {
		super(name, expression, offset, length);
	}

	public String downcasedName() {
		if(downcasedName == null)
			downcasedName = name.toLowerCase(Locale.ENGLISH);
		return downcasedName;
	}
}
