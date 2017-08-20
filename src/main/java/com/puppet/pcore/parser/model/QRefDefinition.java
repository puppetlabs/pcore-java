package com.puppet.pcore.parser.model;

public abstract class QRefDefinition extends Definition {
	public final String name;

	public QRefDefinition(String name, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.name = name;
	}

	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((QRefDefinition)o).name);
	}
}
