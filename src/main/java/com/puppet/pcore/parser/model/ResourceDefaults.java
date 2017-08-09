package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class ResourceDefaults extends AbstractResource {
	public final Expression typeRef;

	public final List<Expression> operations;

	public ResourceDefaults(String form, Expression typeRef, List<Expression> operations, Locator locator, int offset, int length) {
		super(form, locator, offset, length);
		this.typeRef = typeRef;
		this.operations = unmodifiableCopy(operations);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		ResourceDefaults co = (ResourceDefaults)o;
		return typeRef.equals(co.typeRef) && operations.equals(co.operations);
	}
}
