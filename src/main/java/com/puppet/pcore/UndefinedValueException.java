package com.puppet.pcore;

public class UndefinedValueException extends PcoreException {
	public UndefinedValueException() {
		super("undefined value");
	}
}
