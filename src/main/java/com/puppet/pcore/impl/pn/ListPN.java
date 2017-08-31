package com.puppet.pcore.impl.pn;

import com.puppet.pcore.PN;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.map;

public class ListPN extends AbstractPN {
	public final List<? extends PN> elements;

	public ListPN(PN...elements) {
		this.elements = asList(elements);
	}

	public ListPN(List<? extends PN> elements) {
		this.elements = elements;
	}
	@Override
	public PN asCall(String name) {
		return new CallPN(name, elements);
	}

	public boolean equals(Object o) {
		return super.equals(o) && elements.equals(((ListPN)o).elements);
	}

	@Override
	public void format(StringBuilder bld) {
		bld.append('[');
		formatElements(bld);
		bld.append(']');
	}

	@Override
	public Object toData() {
		return map(elements, PN::toData);
	}

	void formatElements(StringBuilder bld) {
		int top = elements.size();
		if(top > 0) {
			elements.get(0).format(bld);
			for(int idx = 1; idx < top; ++idx) {
				bld.append(' ');
				elements.get(idx).format(bld);
			}
		}
	}
}
