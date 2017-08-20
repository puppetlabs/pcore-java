package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.parser.Expression;

public class SelectorEntry extends Positioned {
	public final Expression matching;
	public final Expression value;

	public SelectorEntry(Expression matching, Expression value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.matching = matching;
		this.value = value;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		SelectorEntry co = (SelectorEntry)o;
		return matching.equals(co.matching) && value.equals(co.value);
	}

	@Override
	public PN toPN() {
		return new CallPN("=>", matching.toPN(), value.toPN());
	}
}
