package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.parser.Expression;

public class TypeMapping extends Definition {
	public final Expression type;
	public final Expression mapping;

	public TypeMapping(Expression type, Expression mapping, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.type = type;
		this.mapping = mapping;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		TypeMapping co = (TypeMapping)o;
		return type.equals(co.type) && mapping.equals(co.mapping);
	}

	@Override
	public PN toPN() {
		return new CallPN("type-mapping", type.toPN(), mapping.toPN());
	}
}
