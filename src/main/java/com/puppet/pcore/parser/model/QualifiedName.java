package com.puppet.pcore.parser.model;

public class QualifiedName extends Positioned implements NameExpression {
	public final String name;

	public QualifiedName(String name, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.name = name;
	}

	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((QualifiedName)o).name);
	}

	@Override
	public String name() {
		return name;
	}
}
