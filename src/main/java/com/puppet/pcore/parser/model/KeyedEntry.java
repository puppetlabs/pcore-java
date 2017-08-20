package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.parser.Expression;

public class KeyedEntry extends Positioned {
	public final Expression key;
	public final Expression value;

	public KeyedEntry(Expression key, Expression value, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.key = key;
		this.value = value;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		KeyedEntry co = (KeyedEntry)o;
		return key.equals(co.key) && value.equals(co.value);
	}

	@Override
	public PN toPN() {
		return new CallPN("=>", key.toPN(), value.toPN());
	}
}
