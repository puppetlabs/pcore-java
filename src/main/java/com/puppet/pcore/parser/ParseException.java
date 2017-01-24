package com.puppet.pcore.parser;

import com.puppet.pcore.PCoreException;

public class ParseException extends PCoreException {
	private static final long serialVersionUID = -1;

	public ParseException(String expression, String message, int tokenPos) {
		super(message);
	}
}
