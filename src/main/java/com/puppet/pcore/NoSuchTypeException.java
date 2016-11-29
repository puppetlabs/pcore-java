package com.puppet.pcore;

public class NoSuchTypeException extends PCoreException {
	public NoSuchTypeException(String typeName) {
		super(typeName);
	}
}
