package com.puppet.pcore.parser;

import com.puppet.pcore.PcoreException;

public class ParseException extends PcoreException {
	private static final long serialVersionUID = -1;

	public ParseException(String expression, String message, int tokenPos) {
		super(message);
	}
}
