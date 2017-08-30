package com.puppet.pcore.pspec;

import com.puppet.pcore.Issue;

public enum SpecIssue implements Issue {
	SPEC_EXPRESSION_NOT_PARAMETER_TO("%s can only be a parameter to %s or assigned to a variable"),
	SPEC_ILLEGAL_ARGUMENT_TYPE("Illegal argument type. Function %s, parameter %d expected %s, got %s"),
	SPEC_MISSING_ARGUMENT("Missing required value. Function %s, parameter %d requires a value of type %s"),
	SPEC_NOT_TOP_EXPRESSION("%s is only legal at top level"),
	SPEC_ILLEGAL_CALL_RECEIVER("Illegal call receiver"),
	SPEC_ILLEGAL_NUMBER_OF_ARGUMENTS("Illegal number of arguments. Function %s expects %d arguments, got %d"),
	SPEC_UNKNOWN_IDENTIFIER("unknown identifier %s");

	private final String messageFormat;

	private final boolean demotable;

	SpecIssue(String messageFormat) {
		this.messageFormat = messageFormat;
		this.demotable = false;
	}

	SpecIssue(String messageFormat, boolean demotable) {
		this.messageFormat = messageFormat;
		this.demotable = demotable;
	}

	@Override
	public boolean demotable() {
		return demotable;
	}

	@Override
	public String messageFormat() {
		return messageFormat;
	}
}
