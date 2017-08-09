package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class CaseOption extends Positioned {
	public final List<Expression> values;
	public final Expression then;

	public CaseOption(List<Expression> values, Expression then, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.values = unmodifiableCopy(values);
		this.then = then;
	}

	public boolean equals(Object o) {
		return super.equals(o) && values.equals(((CaseOption)o).values) && then.equals(((CaseOption)o).then);
	}
}
