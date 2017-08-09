package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class ConcatenatedString extends Positioned {
	public final List<Expression> segments;

	public ConcatenatedString(List<Expression> segments, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.segments = unmodifiableCopy(segments);
	}

	public boolean equals(Object o) {
		return super.equals(o) && segments.equals(((ConcatenatedString)o).segments);
	}
}
