package com.puppet.pcore.parser;

import com.puppet.pcore.Issue;

public enum ParseIssue implements Issue {
  LEX_DOUBLE_COLON_NOT_FOLLOWED_BY_NAME(":: not followed by name segment"),
  LEX_DIGIT_EXPECTED("digit expected"),
	LEX_HEREDOC_DECL_UNTERMINATED("unterminated @("),
  LEX_HEREDOC_EMPTY_TAG("empty heredoc tag"),
  LEX_HEREDOC_ILLEGAL_ESCAPE("illegal heredoc escape '%v'"),
  LEX_HEREDOC_MULTIPLE_ESCAPE("more than one declaration of escape flags in heredoc"),
  LEX_HEREDOC_MULTIPLE_SYNTAX("more than one syntax declaration in heredoc"),
  LEX_HEREDOC_MULTIPLE_TAG("more than one tag declaration in heredoc"),
  LEX_HEREDOC_UNTERMINATED("unterminated heredoc"),
	LEX_HEXDIGIT_EXPECTED("hexadecimal digit expected"),
  LEX_INVALID_NAME("invalid name"),
  LEX_INVALID_OPERATOR("invalid operator '%s'"),
  LEX_INVALID_TYPE_NAME("invalid type name"),
  LEX_INVALID_VARIABLE_NAME("invalid variable name"),
  LEX_MALFORMED_INTERPOLATION("malformed interpolation expression"),
  LEX_MALFORMED_UNICODE_ESCAPE("malformed unicode escape sequence"),
  LEX_OCTALDIGIT_EXPECTED("octal digit expected"),
  LEX_UNBALANCED_EPP_COMMENT("unbalanced epp comment"),
  LEX_UNEXPECTED_TOKEN("unexpected token '%s'"),
  LEX_UNTERMINATED_COMMENT("unterminated /* */ comment"),
  LEX_UNTERMINATED_STRING("unterminated %s quoted string"),

  PARSE_CLASS_NOT_VALID_HERE("'class' keyword not allowed at this location"),
  PARSE_ELSIF_IN_UNLESS("elsif not supported in unless expression"),
  PARSE_EXPECTED_ATTRIBUTE_NAME("expected attribute name"),
  PARSE_EXPECTED_CLASS_NAME("expected name of class"),
  PARSE_EXPECTED_FARROW_AFTER_KEY("expected '=>' to follow hash key"),
	PARSE_EXPECTED_HOSTNAME("hostname expected"),
  PARSE_EXPECTED_NAME_OR_NUMBER_AFTER_DOT("expected name or number to follow '.'"),
  PARSE_EXPECTED_NAME_AFTER_FUNCTION("expected a name to follow keyword 'function'"),
	PARSE_EXPECTED_ONE_OF_TOKENS("expected one of %s, got '%s'"),
  PARSE_EXPECTED_TITLE("resource title expected"),
  PARSE_EXPECTED_TOKEN("expected token '%s', got '%s'"),
  PARSE_EXPECTED_TYPE_NAME("expected type name"),
	PARSE_EXPECTED_TYPE_NAME_AFTER_TYPE("expected type name to follow 'type'"),
  PARSE_EXPECTED_VARIABLE("expected variable declaration"),
  PARSE_ILLEGAL_EPP_PARAMETERS("Ambiguous EPP parameter expression. Probably missing '<%%-' before parameters to remove leading whitespace"),
	PARSE_INVALID_ATTRIBUTE("invalid attribute operation"),
  PARSE_INVALID_RESOURCE("invalid resource expression"),
  PARSE_INHERITS_MUST_BE_TYPE_NAME("expected type name to follow 'inherits'"),
  PARSE_RESOURCE_WITHOUT_TITLE("This expression is invalid. Did you try declaring a '%s' resource without a title?"),
  PARSE_QUOTED_NOT_VALID_NAME("a quoted string is not valid as a name at this location");

	private final String messageFormat;

	private final boolean demotable;

	ParseIssue(String messageFormat) {
		this.messageFormat = messageFormat;
		this.demotable = false;
	}

	ParseIssue(String messageFormat, boolean demotable) {
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
