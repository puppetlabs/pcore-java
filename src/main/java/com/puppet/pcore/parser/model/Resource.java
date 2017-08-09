package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class Resource extends AbstractResource {
	public final Expression typeName;

	public final List<ResourceBody> bodies;

	public Resource(String form, Expression typeName, List<ResourceBody> bodies, Locator locator, int offset, int length) {
		super(form, locator, offset, length);
		this.typeName = typeName;
		this.bodies = unmodifiableCopy(bodies);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		Resource co = (Resource)o;
		return typeName.equals(co.typeName) && bodies.equals(co.bodies);
	}
}
