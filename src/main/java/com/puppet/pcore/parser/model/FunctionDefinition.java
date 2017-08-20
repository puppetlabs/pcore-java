package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.ListPN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.map;

public class FunctionDefinition extends NamedDefinition {
	public final Expression returnType;

	public FunctionDefinition(String name, List<Parameter> parameters, Expression body, Expression returnType, Locator locator, int offset, int length) {
		super(name, parameters, body, locator, offset, length);
		this.returnType = returnType;
	}

	public boolean equals(Object o) {
		return super.equals(o) && Objects.equals(returnType, ((FunctionDefinition)o).returnType);
	}

	@Override
	public PN toPN() {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(new LiteralPN(name).withName("name"));
		entries.add(new ListPN(map(parameters, Expression::toPN)).withName("params"));
		entries.add(body.toPN().withName("body"));
		if(returnType != null)
			entries.add(returnType.toPN().withName("returns"));
		return new MapPN(entries).asCall("function");
	}
}
