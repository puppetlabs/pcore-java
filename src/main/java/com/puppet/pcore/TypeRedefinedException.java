package com.puppet.pcore;

public class TypeRedefinedException extends PcoreException {
	private static final long serialVersionUID = -1;

	public TypeRedefinedException(String typeName) {
		super(typeName);
	}
}
