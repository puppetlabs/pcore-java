package com.puppet.pcore.parser.model;

public abstract class AbstractResource extends Positioned {
	public final String form;

	public AbstractResource(String form, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.form = form;
	}

	public boolean equals(Object o) {
		return super.equals(o) && form.equals(((AbstractResource)o).form);
	}
}
