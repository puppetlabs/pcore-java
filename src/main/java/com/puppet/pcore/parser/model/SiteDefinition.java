package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

public class SiteDefinition extends Definition {
	public final Expression body;

	public SiteDefinition(Expression body, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.body = body;
	}

	public boolean equals(Object o) {
		return super.equals(o) && body.equals(((SiteDefinition)o).body);
	}
}
