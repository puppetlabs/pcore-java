package com.puppet.pcore.impl.pn;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.parser.Lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static com.puppet.pcore.impl.parser.LexTokens.*;

public class PNParser extends Lexer {
	public static PN parse(String file, String content) {
		return new PNParser(file, content).parseNext();
	}

	private PNParser(String file, String content) {
		super(file, content, false);
		nextToken();
	}

	private PN parseNext() {
		switch(currentToken()) {
		case TOKEN_LB: case TOKEN_LISTSTART:
			return parseArray();
		case TOKEN_LC: case TOKEN_SELC:
			return parseMap();
		case TOKEN_LP: case TOKEN_WSLP:
			return parseCall();
		case TOKEN_STRING: case TOKEN_BOOLEAN: case TOKEN_INTEGER: case TOKEN_FLOAT: case TOKEN_UNDEF:
			return parseLiteral();
		case TOKEN_IDENTIFIER:
			switch(tokenString()) {
			case "null":
				return new LiteralPN(null);
			}
			break;
		case TOKEN_SUBTRACT:
			switch(nextToken()) {
			case TOKEN_FLOAT:
				return new LiteralPN(-((Number)tokenValue()).doubleValue());
			case TOKEN_INTEGER:
				return new LiteralPN(-((Number)tokenValue()).longValue());
			}
		}
		throw syntaxError();
	}

	private PN parseArray() {
		return new ListPN(parseElements(TOKEN_RB));
	}

	private PN parseMap() {
		List<Entry<String,? extends PN>> entries = new ArrayList<>();
		int token = nextToken();
		while(token != TOKEN_RC && token != TOKEN_END) {
			assertToken(TOKEN_COLON);
			nextToken();
			String key = parseIdentifier();
			entries.add(parseNext().withName(key));
			token = currentToken();
		}
		assertToken(TOKEN_RC);
		nextToken();
		return new MapPN(entries);
	}

	private PN parseCall() {
		nextToken();
		String name = parseIdentifier();
		return new CallPN(name, parseElements(TOKEN_RP));
	}

	private PN parseLiteral() {
		PN pn = new LiteralPN(tokenValue());
		nextToken();
		return pn;
	}

	private String parseIdentifier() {
		switch(currentToken()) {
		case TOKEN_END:
    case TOKEN_LP: case TOKEN_WSLP: case TOKEN_RP:
    case TOKEN_LB: case TOKEN_LISTSTART: case TOKEN_RB:
    case TOKEN_LC: case TOKEN_SELC: case TOKEN_RC:
    case TOKEN_EPP_END: case TOKEN_EPP_END_TRIM: case TOKEN_RENDER_EXPR: case TOKEN_RENDER_STRING:
    case TOKEN_COMMA: case TOKEN_COLON: case TOKEN_SEMICOLON:
    case TOKEN_STRING: case TOKEN_INTEGER: case TOKEN_FLOAT: case TOKEN_CONCATENATED_STRING: case TOKEN_HEREDOC:
    case TOKEN_REGEXP:
    	break;
		case TOKEN_DEFAULT:
			nextToken();
			return "default";
		default:
			String str = tokenString();
			nextToken();
			return str;
		}
		throw syntaxError();
	}

	private List<PN> parseElements(int endToken) {
		List<PN> elements = new ArrayList<>();
		int token = currentToken();
		while(token != endToken && token != TOKEN_END) {
			elements.add(parseNext());
			token = currentToken();
		}
		assertToken(endToken);
		nextToken();
		return elements;
	}
}
