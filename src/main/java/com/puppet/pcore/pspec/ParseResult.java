package com.puppet.pcore.pspec;

import com.puppet.pcore.parser.Expression;

public class ParseResult implements Result<Expression> {
	public final Assertions assertions;

	public final String result;

	public ParseResult(Assertions assertions, String result) {
		this.assertions = assertions;
		this.result = result;
	}

	@Override
	public Executable createTest(Expression actual) {
		return () -> assertions.assertEquals(result, actual.toPN().toString());
	}
}
