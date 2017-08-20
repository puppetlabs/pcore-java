package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class ResourceBody extends Positioned {
	public final Expression title;

	public final List<Expression> operations;

	public ResourceBody(Expression title, List<Expression> operations, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.title = title;
		this.operations = unmodifiableCopy(operations);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		ResourceBody co = (ResourceBody)o;
		return title.equals(co.title) && operations.equals(co.operations);
	}

	@Override
	public PN toPN() {
		return new MapPN(
			title.toPN().withName("title"),
			pnList(operations).withName("ops")
		);
	}
}
