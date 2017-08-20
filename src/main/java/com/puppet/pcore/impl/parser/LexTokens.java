package com.puppet.pcore.impl.parser;

import com.puppet.pcore.impl.Helpers;

import java.util.Map;

public interface LexTokens {

  int TOKEN_END = 0;

  // Binary ops
  int TOKEN_ASSIGN = 1;
  int TOKEN_ADD_ASSIGN = 2;
  int TOKEN_SUBTRACT_ASSIGN = 3;

  int TOKEN_MULTIPLY = 10;
  int TOKEN_DIVIDE = 11;
  int TOKEN_REMAINDER = 12;
  int TOKEN_SUBTRACT = 13;
  int TOKEN_ADD = 14;

  int TOKEN_LSHIFT = 20;
  int TOKEN_RSHIFT = 21;

  int TOKEN_EQUAL = 30;
  int TOKEN_NOT_EQUAL = 31;
  int TOKEN_LESS = 32;
  int TOKEN_LESS_EQUAL = 33;
  int TOKEN_GREATER = 34;
  int TOKEN_GREATER_EQUAL = 35;

  int TOKEN_MATCH = 40;
  int TOKEN_NOT_MATCH = 41;

  int TOKEN_LCOLLECT = 50;
  int TOKEN_LLCOLLECT = 51;

  int TOKEN_RCOLLECT = 60;
  int TOKEN_RRCOLLECT = 61;

  int TOKEN_FARROW = 70;
  int TOKEN_PARROW = 71;

  int TOKEN_IN_EDGE = 72;
  int TOKEN_IN_EDGE_SUB = 73;
  int TOKEN_OUT_EDGE = 74;
  int TOKEN_OUT_EDGE_SUB = 75;

  // Unary ops
  int TOKEN_NOT = 80;
  int TOKEN_AT = 81;
  int TOKEN_ATAT = 82;

  // ()
  int TOKEN_LP = 90;
  int TOKEN_WSLP = 91;
  int TOKEN_RP = 92;

  // []
  int TOKEN_LB = 100;
  int TOKEN_LISTSTART = 101;
  int TOKEN_RB = 102;

  // {}
  int TOKEN_LC = 110;
  int TOKEN_SELC = 111;
  int TOKEN_RC = 112;

  // | |
  int TOKEN_PIPE = 120;

  // EPP
  int TOKEN_EPP_END = 130;
  int TOKEN_EPP_END_TRIM = 131;
  int TOKEN_RENDER_EXPR = 132;
  int TOKEN_RENDER_STRING = 133;

  // Separators
	int TOKEN_COMMA = 140;
  int TOKEN_DOT = 141;
  int TOKEN_QMARK = 142;
  int TOKEN_COLON = 143;
  int TOKEN_SEMICOLON = 144;

  // Strings with semantics
  int TOKEN_IDENTIFIER = 150;
  int TOKEN_STRING = 151;
  int TOKEN_INTEGER = 152;
  int TOKEN_FLOAT = 153;
  int TOKEN_BOOLEAN = 154;
  int TOKEN_CONCATENATED_STRING = 155;
  int TOKEN_HEREDOC = 156;
  int TOKEN_VARIABLE = 157;
  int TOKEN_REGEXP = 158;
  int TOKEN_TYPE_NAME = 159;

  // Keywords
  int TOKEN_AND = 200;
  int TOKEN_APPLICATION = 201;
  int TOKEN_ATTR = 202;
  int TOKEN_CASE = 203;
  int TOKEN_CLASS = 204;
  int TOKEN_CONSUMES = 205;
  int TOKEN_DEFAULT = 206;
  int TOKEN_DEFINE = 207;
  int TOKEN_FUNCTION = 208;
  int TOKEN_IF = 209;
  int TOKEN_IN = 210;
  int TOKEN_INHERITS = 211;
  int TOKEN_ELSE = 212;
  int TOKEN_ELSIF = 213;
  int TOKEN_NODE = 214;
  int TOKEN_OR = 215;
  int TOKEN_PRIVATE = 216;
  int TOKEN_PRODUCES = 217;
  int TOKEN_SITE = 218;
  int TOKEN_TYPE = 219;
  int TOKEN_UNDEF = 220;
  int TOKEN_UNLESS = 221;

	Map<Integer,String> tokenMap = Helpers.asMap(
			TOKEN_END, "EOF",

			// Binary ops
			TOKEN_ASSIGN, "=",
			TOKEN_ADD_ASSIGN, "+=",
			TOKEN_SUBTRACT_ASSIGN, "-=",

			TOKEN_MULTIPLY, "*",
			TOKEN_DIVIDE, "/",
			TOKEN_REMAINDER, "%",
			TOKEN_SUBTRACT, "-",
			TOKEN_ADD, "+",

			TOKEN_LSHIFT, "<<",
			TOKEN_RSHIFT, ">>",

			TOKEN_EQUAL, "==",
			TOKEN_NOT_EQUAL, "!=",
			TOKEN_LESS, "<",
			TOKEN_LESS_EQUAL, "<=",
			TOKEN_GREATER, ">",
			TOKEN_GREATER_EQUAL, ">=",

			TOKEN_MATCH, "=~",
			TOKEN_NOT_MATCH, "!~",

			TOKEN_LCOLLECT, "<|",
			TOKEN_LLCOLLECT, "<<|",

			TOKEN_RCOLLECT, "|>",
			TOKEN_RRCOLLECT, "|>>",

			TOKEN_FARROW, "=>",
			TOKEN_PARROW, "+>",

			TOKEN_IN_EDGE, "->",
			TOKEN_IN_EDGE_SUB, "~>",
			TOKEN_OUT_EDGE, "<-",
			TOKEN_OUT_EDGE_SUB, "<~",

			// Unary ops
			TOKEN_NOT, "!",
			TOKEN_AT, "@",
			TOKEN_ATAT, "@@",

			TOKEN_COMMA, ",",

			// ()
			TOKEN_LP, "(",
			TOKEN_WSLP, "(",
			TOKEN_RP, ")",

			// []
			TOKEN_LB, "[",
			TOKEN_LISTSTART, "[",
			TOKEN_RB, "]",

			// {}
			TOKEN_LC, "{",
			TOKEN_SELC, "{",
			TOKEN_RC, "}",

			// | |
			TOKEN_PIPE, "|",

			// EPP
			TOKEN_EPP_END, "%>",
			TOKEN_EPP_END_TRIM, "-%>",
			TOKEN_RENDER_EXPR, "<%=",
			TOKEN_RENDER_STRING, "epp text",

			// Separators
			TOKEN_DOT, ".",
			TOKEN_QMARK, "?",
			TOKEN_COLON, ":",
			TOKEN_SEMICOLON, ";",

			// Strings with semantics
			TOKEN_IDENTIFIER, "<identifier>",
			TOKEN_STRING, "<string literal>",
			TOKEN_INTEGER, "<integer literal>",
			TOKEN_FLOAT, "<float literal>",
			TOKEN_BOOLEAN, "<boolean literal>",
			TOKEN_CONCATENATED_STRING, "<dq string literal>",
			TOKEN_HEREDOC, "<heredoc>",
			TOKEN_VARIABLE, "<variable>",
			TOKEN_REGEXP, "<regexp>",
			TOKEN_TYPE_NAME, "<type name>",

			// Keywords
			TOKEN_AND, "and",
			TOKEN_APPLICATION, "application",
			TOKEN_ATTR, "attr",
			TOKEN_CASE, "case",
			TOKEN_CLASS, "class",
			TOKEN_CONSUMES, "consumes",
			TOKEN_DEFAULT, "default",
			TOKEN_DEFINE, "define",
			TOKEN_FUNCTION, "function",
			TOKEN_IF, "if",
			TOKEN_IN, "in",
			TOKEN_INHERITS, "inherits",
			TOKEN_ELSE, "else",
			TOKEN_ELSIF, "elsif",
			TOKEN_NODE, "node",
			TOKEN_OR, "or",
			TOKEN_PRIVATE, "private",
			TOKEN_PRODUCES, "produces",
			TOKEN_SITE, "site",
			TOKEN_TYPE, "type",
			TOKEN_UNDEF, "undef",
			TOKEN_UNLESS, "unless"
	);

	Map<String,Integer> keywords = Helpers.asMap(
		  tokenMap.get(TOKEN_APPLICATION), TOKEN_APPLICATION,
		  tokenMap.get(TOKEN_AND), TOKEN_AND,
			tokenMap.get(TOKEN_ATTR), TOKEN_ATTR,
		  tokenMap.get(TOKEN_CASE), TOKEN_CASE,
		  tokenMap.get(TOKEN_CLASS), TOKEN_CLASS,
		  tokenMap.get(TOKEN_CONSUMES), TOKEN_CONSUMES,
		  tokenMap.get(TOKEN_DEFAULT), TOKEN_DEFAULT,
		  tokenMap.get(TOKEN_DEFINE), TOKEN_DEFINE,
		  "false", TOKEN_BOOLEAN,
		  tokenMap.get(TOKEN_FUNCTION), TOKEN_FUNCTION,
		  tokenMap.get(TOKEN_ELSE), TOKEN_ELSE,
		  tokenMap.get(TOKEN_ELSIF), TOKEN_ELSIF,
		  tokenMap.get(TOKEN_IF), TOKEN_IF,
		  tokenMap.get(TOKEN_IN), TOKEN_IN,
		  tokenMap.get(TOKEN_INHERITS), TOKEN_INHERITS,
		  tokenMap.get(TOKEN_NODE), TOKEN_NODE,
		  tokenMap.get(TOKEN_OR), TOKEN_OR,
			tokenMap.get(TOKEN_PRIVATE), TOKEN_PRIVATE,
		  tokenMap.get(TOKEN_PRODUCES), TOKEN_PRODUCES,
		  tokenMap.get(TOKEN_SITE), TOKEN_SITE,
		  "true", TOKEN_BOOLEAN,
		  tokenMap.get(TOKEN_TYPE), TOKEN_TYPE,
		  tokenMap.get(TOKEN_UNDEF), TOKEN_UNDEF,
		  tokenMap.get(TOKEN_UNLESS), TOKEN_UNLESS
	);
}
