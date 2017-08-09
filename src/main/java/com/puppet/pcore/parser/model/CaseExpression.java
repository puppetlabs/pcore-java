package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class CaseExpression extends Positioned {
	public final Expression test;
	public final List<CaseOption> options;

	public CaseExpression(Expression test, List<CaseOption> options, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.test = test;
		this.options = unmodifiableCopy(options);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		CaseExpression co = (CaseExpression)o;
		return test.equals(co.test) && options.equals(co.options);
	}
}
