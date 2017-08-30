package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;

public class ReservedWord extends LiteralExpression implements NameExpression {
	public final String word;

	public final boolean future;

	public ReservedWord(String word, boolean future, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.word = word;
		this.future = future;
	}

	public boolean equals(Object o) {
		return super.equals(o) && future == ((ReservedWord)o).future && word.equals(((ReservedWord)o).word);
	}

	@Override
	public String name() {
		return word;
	}

	@Override
	public PN toPN() {
		return new LiteralPN(word).asCall("reserved");
	}

	@Override
	public Object value() {
		return word;
	}
}
