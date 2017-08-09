package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public abstract class Positioned implements Expression {
	public final Locator locator;

	public final int offset;

	public final int length;

	public Positioned(Locator locator, int offset, int length) {
		this.locator = locator;
		this.offset = offset;
		this.length = length;
	}

	public boolean equals(Object o) {
		return o != null && getClass().equals(o.getClass());
	}

	@Override
	public int offset() {
		return offset;
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public String toString() {
		return locator.source.substring(offset, offset + length);
	}
}
