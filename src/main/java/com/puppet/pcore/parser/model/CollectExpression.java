package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class CollectExpression extends Positioned {
	public final Expression resourceType;

	public final Expression query;

	public final List<Expression> operations;

	public CollectExpression(Expression resourceType, Expression query, List<Expression> operations, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.resourceType = resourceType;
		this.query = query;
		this.operations = unmodifiableCopy(operations);
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		CollectExpression co = (CollectExpression)o;
		return resourceType.equals(co.resourceType) && Objects.equals(query, co.query) && operations.equals(co.operations);
	}

	@Override
	public PN toPN() {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(resourceType.toPN().withName("type"));
		entries.add(query.toPN().withName("query"));
		if(!operations.isEmpty())
			entries.add(pnList(operations).withName("ops"));
		return new MapPN(entries).asCall("collect");
	}
}
