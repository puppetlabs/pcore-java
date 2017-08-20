package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class ArrayExpression extends Positioned {
	public final List<Expression> elements;

	public ArrayExpression(List<Expression> elements, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.elements = unmodifiableCopy(elements);
	}

	public boolean equals(Object o) {
		return super.equals(o) && elements.equals(((ArrayExpression)o).elements);
	}

	@Override
	public PN toPN() {
		return pnList(elements).asCall("array");
	}
}
