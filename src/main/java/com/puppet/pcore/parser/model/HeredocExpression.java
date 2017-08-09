package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.Objects;

public class HeredocExpression extends Positioned {
	public final Expression text;

	public final String syntax;

	public HeredocExpression(Expression text, String syntax, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.text = text;
		this.syntax = syntax;
	}

	public boolean equals(Object o) {
		return super.equals(o) && text.equals(((HeredocExpression)o).text) && Objects.equals(syntax, ((HeredocExpression)o).syntax);
	}
}
