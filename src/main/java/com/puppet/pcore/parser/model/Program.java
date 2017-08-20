package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class Program extends Positioned {
	public final Expression body;

	public final List<Definition> definitions;

	public Program(Expression body, List<Definition> definitions, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.body = body;
		this.definitions = unmodifiableCopy(definitions);
	}

	public boolean equals(Object o) {
		return super.equals(o) && body.equals(((Program)o).body);
	}

	@Override
	public PN toPN() {
		return body.toPN();
	}
}
