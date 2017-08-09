package com.puppet.pcore.parser.model;

import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class CapabilityMapping extends Definition {
	public final String kind;

	public final String capability;

	public final Expression component;

	public final List<Expression> mappings;

	public CapabilityMapping(String kind, String capability, Expression component, List<Expression> mappings, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.kind = kind;
		this.capability = capability;
		this.component = component;
		this.mappings = unmodifiableCopy(mappings);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		CapabilityMapping co = (CapabilityMapping)o;
		return kind.equals(co.kind) && capability.equals(co.capability) && component.equals(co.component) && mappings.equals(co.mappings);
	}
}
