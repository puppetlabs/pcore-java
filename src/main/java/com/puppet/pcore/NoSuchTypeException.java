package com.puppet.pcore;

public class NoSuchTypeException extends PCoreException {
	private static final long serialVersionUID = -1;

	public NoSuchTypeException(String typeName) {
		super(typeName);
	}
}
