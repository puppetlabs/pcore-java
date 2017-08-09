package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class IfExpression extends Positioned {
	public final Expression test;
	public final Expression then;
	public final Expression elseExpr;

	public IfExpression(Expression test, Expression then, Expression elseExpr, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.test = test;
		this.then = then;
		this.elseExpr = elseExpr;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		IfExpression co = (IfExpression)o;
		return test.equals(co.test) && Objects.equals(then, co.then) && Objects.equals(elseExpr, co.elseExpr);
	}
}
