package com.puppet.pcore.parser.model;

public abstract class Definition extends Positioned {
	public Definition(Locator locator, int offset, int length) {
		super(locator, offset, length);
	}
}
