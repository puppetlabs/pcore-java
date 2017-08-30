package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Parameter extends Positioned {
	public final String name;

	public final Expression value;

	public final Expression type;

	public final boolean capturesRest;

	public Parameter(String name, Expression type, Expression value, boolean capturesRest, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.name = name;
		this.type = type;
		this.value = value;
		this.capturesRest = capturesRest;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		Parameter co = (Parameter)o;
		return capturesRest == co.capturesRest && name.equals(co.name) && Objects.equals(type, co.type) && Objects.equals(value, co.value);
	}

	@Override
	public PN toPN() {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(new LiteralPN(name).withName("name"));
		if(type != null)
			entries.add(type.toPN().withName("type"));
		if(capturesRest)
			entries.add(new LiteralPN(name).withName("splat"));
		if(value != null)
			entries.add(value.toPN().withName("value"));
		return new MapPN(entries).asCall("param");
	}
}
