package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class SelectorExpression extends Positioned {
	public final Expression lhs;
	public final List<SelectorEntry> options;

	public SelectorExpression(Expression lhs, List<SelectorEntry> options, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.lhs = lhs;
		this.options = unmodifiableCopy(options);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		SelectorExpression co = (SelectorExpression)o;
		return lhs.equals(co.lhs) && options.equals(co.options);
	}
}
