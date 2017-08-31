package com.puppet.pcore.impl.pn;

import com.puppet.pcore.PN;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.concat;
import static com.puppet.pcore.impl.Helpers.map;

public class CallPN extends ListPN {
	public final String identifier;

	public CallPN(String identifier, PN...args) {
		super(args);
		this.identifier = identifier;
	}

	public CallPN(String identifier, List<? extends PN> args) {
		super(args);
		this.identifier = identifier;
	}

	public boolean equals(Object o) {
		return super.equals(o) && identifier.equals(((CallPN)o).identifier);
	}

	@Override
	public void format(StringBuilder bld) {
		bld.append('(');
		bld.append(identifier);
		if(!elements.isEmpty()) {
			bld.append(' ');
			formatElements(bld);
		}
		bld.append(')');
	}

	@Override
	public Object toData() {
		return concat(asList(identifier), map(elements, PN::toData));
	}

	@Override
	public PN asCall(String name) {
		return new CallPN(name, elements);
	}
}
