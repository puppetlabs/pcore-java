package com.puppet.pcore.impl.parser;

import com.puppet.pcore.PCoreException;

public class ParseException extends PCoreException {
	public ParseException(String expression, String message, int tokenPos) {
		super(message);
	}
}
