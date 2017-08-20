package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class ResourceOverride extends AbstractResource {
	public final Expression resources;

	public final List<Expression> operations;

	public ResourceOverride(String form, Expression resources, List<Expression> operations, Locator locator, int offset, int length) {
		super(form, locator, offset, length);
		this.resources = resources;
		this.operations = unmodifiableCopy(operations);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		ResourceOverride co = (ResourceOverride)o;
		return resources.equals(co.resources) && operations.equals(co.operations);
	}

	@Override
	public PN toPN() {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(resources.toPN().withName("resources"));
		entries.add(pnList(operations).withName("ops"));
		if(!form.equals("regular"))
			entries.add(new LiteralPN(form).withName("form"));
		return new MapPN(entries).asCall("resource-override");
	}
}
