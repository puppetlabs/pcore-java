package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	@Override
	public PN toPN() {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(typeRef.toPN().withName("type"));
		entries.add(pnList(operations).withName("ops"));
		if(!form.equals("regular"))
			entries.add(new LiteralPN(form).withName("form"));
		return new MapPN(entries).asCall("resource-defaults");
	}
}
