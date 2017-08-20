package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;

public class QualifiedReference extends Positioned implements NameExpression {
	public final String name;

	private String downcasedName;

	public QualifiedReference(String name, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.name = name;
	}

	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((QualifiedReference)o).name);
	}

	public String downcasedName() {
		if(downcasedName == null)
			downcasedName = name.toLowerCase();
		return downcasedName;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public PN toPN() {
		return new LiteralPN(name).asCall("qr");
	}
}
