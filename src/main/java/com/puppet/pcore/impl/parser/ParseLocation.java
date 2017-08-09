package com.puppet.pcore.impl.parser;

import com.puppet.pcore.Location;
import com.puppet.pcore.parser.model.Locator;

class ParseLocation implements Location {
	final Locator locator;

	final int offset;

	ParseLocation(Locator locator, int offset) {
		this.locator = locator;
		this.offset = offset;
	}

	@Override
	public String sourceName() {
		return locator.file;
	}

	@Override
	public int line() {
		return locator.lineforOffset(offset);
	}

	@Override
	public int pos() {
		return locator.posforOffset(offset);
	}
}
