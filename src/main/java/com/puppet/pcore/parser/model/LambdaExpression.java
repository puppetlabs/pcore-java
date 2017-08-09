package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class LambdaExpression extends Definition {
	public final List<Parameter> parameters;

	public final Expression returnType;

	public final Expression body;

	public LambdaExpression(List<Parameter> parameters, Expression returnType, Expression body, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.parameters = unmodifiableCopy(parameters);
		this.returnType = returnType;
		this.body = body;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		LambdaExpression co = (LambdaExpression)o;
		return parameters.equals(co.parameters) && body.equals(co.body) && Objects.equals(returnType, co.returnType);
	}
}
