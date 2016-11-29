package com.puppet.pcore.impl.parser;

import com.puppet.pcore.Default;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class DefaultExpressionParser implements com.puppet.pcore.parser.ExpressionParser {
	private interface EscapeHandler {
		int handle(StringBuilder buf, int tokenPos, char c);
	}

	private static final String KEYWORD_FALSE = "false";
	private static final String KEYWORD_TRUE = "true";
	private static final String KEYWORD_DEFAULT = "default";
	private static final String KEYWORD_UNDEF = "undef";
	private static final String OPERATOR_ARROW = "=>";
	private static final String OPERATOR_SEPARATOR = "::";
	private static final int TOKEN_ARROW = 10;
	private static final int TOKEN_SEPARATOR = 11;
	private static final int TOKEN_COMMA = 12;
	private static final int TOKEN_MINUS = 13;
	private static final int TOKEN_UNARY_MINUS = 14;
	private static final int TOKEN_ASSIGN = 15;
	private static final int TOKEN_LP = 30;
	private static final int TOKEN_RP = 31;
	private static final int TOKEN_LB = 32;
	private static final int TOKEN_RB = 33;
	private static final int TOKEN_LC = 34;
	private static final int TOKEN_RC = 35;
	private static final int TOKEN_TYPE_NAME_SEGMENT = 40;
	private static final int TOKEN_IDENTIFIER_SEGMENT = 41;
	private static final int TOKEN_LITERAL = 42;
	private static final int TOKEN_REGEXP = 43;
	private static final int TOKEN_UNDEF = 50;
	private static final int TOKEN_TRUE = 51;
	private static final int TOKEN_FALSE = 52;
	private static final int TOKEN_DEFAULT = 53;
	private static final int TOKEN_END = 0;
	private static final int TOKEN_ERROR = -1;
	private static final Map<String,Integer> keywords;

	static {
		keywords = new HashMap<>();
		keywords.put(KEYWORD_FALSE, TOKEN_FALSE);
		keywords.put(KEYWORD_UNDEF, TOKEN_UNDEF);
		keywords.put(KEYWORD_TRUE, TOKEN_TRUE);
		keywords.put(KEYWORD_DEFAULT, TOKEN_DEFAULT);
	}

	private final ExpressionFactory factory;
	private int currentToken;
	private String expression;
	private int lastTokenPos;
	private int tokenPos;
	private Object tokenValue;

	public DefaultExpressionParser(ExpressionFactory factory) {
		this.factory = factory;
	}

	@Override
	public synchronized Expression parse(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		skipWhite();
		int start = tokenPos;
		nextToken();
		Expression expr = currentToken == TOKEN_END
				? factory.constant(null, expression, start, expression.length())
				: parseBinary(start);
		assertToken(TOKEN_END);
		return expr;
	}

	@SuppressWarnings("WeakerAccess")
	protected Map<String,Integer> keywordToTokenMap() {
		return keywords;
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isHexDigit(char c) {
		return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
	}

	private static boolean isLetter(char c) {
		return isLowercaseLetter(c) || isUppercaseLetter(c);
	}

	private static boolean isLetterOrDigit(char c) {
		return isLetter(c) || isDigit(c) || c == '_';
	}

	private static boolean isLowercaseLetter(char c) {
		return c >= 'a' && c <= 'z';
	}

	private static boolean isOctalDigit(char c) {
		return c >= '0' && c <= '7';
	}

	private static boolean isUppercaseLetter(char c) {
		return c >= 'A' && c <= 'Z';
	}

	private void assertToken(int token) {
		if(currentToken != token)
			throw syntaxError();
	}

	private void findIntEnd() {
		int top = expression.length();
		while(tokenPos < top && Character.isDigit(expression.charAt(tokenPos)))
			++tokenPos;
	}

	private void nextToken() {
		tokenValue = null;
		int top = expression.length();
		char c = skipWhite();
		if(tokenPos >= top) {
			lastTokenPos = top;
			currentToken = TOKEN_END;
			return;
		}

		lastTokenPos = tokenPos;
		switch(c) {
		case '=':
			if(tokenPos + 1 < top) {
				switch(expression.charAt(tokenPos + 1)) {
				case '>':
					tokenValue = OPERATOR_ARROW;
					currentToken = TOKEN_ARROW;
					tokenPos += 2;
					break;
				default:
					currentToken = TOKEN_ASSIGN;
					++tokenPos;
					break;
				}
			} else
				currentToken = TOKEN_ERROR;
			break;

		case '{':
			currentToken = TOKEN_LC;
			++tokenPos;
			break;

		case '}':
			currentToken = TOKEN_RC;
			++tokenPos;
			break;

		case '[':
			currentToken = TOKEN_LB;
			++tokenPos;
			break;

		case ']':
			currentToken = TOKEN_RB;
			++tokenPos;
			break;

		case ',':
			currentToken = TOKEN_COMMA;
			++tokenPos;
			break;

		case ':':
			if(tokenPos + 1 < top) {
				c = expression.charAt(tokenPos + 1);
				if(c == ':') {
					currentToken = TOKEN_SEPARATOR;
					tokenPos += 2;
					break;
				}
			}
			currentToken = TOKEN_ERROR;
			break;

		case '-':
			if(tokenPos + 1 < top) {
				c = expression.charAt(tokenPos + 1);
				switch(c) {
				case ' ':
				case '\t':
				case '\r':
				case '\n':
					currentToken = TOKEN_MINUS;
					++tokenPos;
				case '(':
					currentToken = TOKEN_UNARY_MINUS;
					++tokenPos;
				default:
					currentToken = isLetterOrDigit(c) ? TOKEN_UNARY_MINUS : TOKEN_MINUS;
					++tokenPos;
				}
			} else {
				currentToken = TOKEN_MINUS;
				++tokenPos;
			}
			break;

		case '"':
			parseDoubleQuotedString();
			break;

		case '\'':
			parseSingleQuotedString();
			break;

		case '/':
			parseRegexpString();
			if(currentToken == TOKEN_LITERAL)
				currentToken = TOKEN_REGEXP;
			break;

		case '0':
			if(++tokenPos >= top) {
				tokenValue = 0;
				currentToken = TOKEN_LITERAL;
				break;
			}
			c = expression.charAt(tokenPos);
			if(c == 'x' || c == 'X') {
				if(tokenPos + 1 >= top)
					throw syntaxError();
				int start = ++tokenPos;
				c = expression.charAt(tokenPos);
				if(!isHexDigit(c))
					throw syntaxError();
				while(++tokenPos < top)
					if(!isHexDigit(expression.charAt(tokenPos)))
						break;
				tokenValue = Long.valueOf(expression.substring(start, tokenPos), 16);
				currentToken = TOKEN_LITERAL;
			} else if(isOctalDigit(c)) {
				int start = tokenPos;
				while(++tokenPos < top)
					if(!isOctalDigit(expression.charAt(tokenPos)))
						break;
				tokenValue = Long.valueOf(expression.substring(start, tokenPos), 8);
				currentToken = TOKEN_LITERAL;
			} else if(c == '.' || c == 'e' || c == 'E') {
				parseDouble(tokenPos - 1, c);
				break;
			} else {
				tokenValue = 0;
				currentToken = TOKEN_LITERAL;
			}
			break;
		default:
			if(isDigit(c)) {
				int start = tokenPos++;
				findIntEnd();
				if(tokenPos + 1 < top) {
					c = expression.charAt(tokenPos);
					if(c == '.' || c == 'e' || c == 'E') {
						parseDouble(start, c);
						break;
					}
				}
				tokenValue = Long.valueOf(expression.substring(start, tokenPos));
				currentToken = TOKEN_LITERAL;
				break;
			}
			if(isLetter(c)) {
				int start = tokenPos++;
				while(tokenPos < top && isLetterOrDigit(expression.charAt(tokenPos)))
					++tokenPos;
				String word = expression.substring(start, tokenPos);
				Integer token = keywordToTokenMap().get(word);
				if(token == null)
					currentToken = isUppercaseLetter(c) ? TOKEN_TYPE_NAME_SEGMENT : TOKEN_IDENTIFIER_SEGMENT;
				else
					currentToken = token;
				tokenValue = word;
				break;
			}
			throw syntaxError();
		}
	}

	private List<Expression> parseArray(int start) {
		ArrayList<Expression> operands = new ArrayList<>();
		if(currentToken != TOKEN_RB) {
			operands.add(parseBinary(start));
			while(currentToken == TOKEN_COMMA) {
				start = tokenPos;
				nextToken();
				if(currentToken == TOKEN_RC)
					// Extraneous comma causes this. That's OK
					break;
				operands.add(parseBinary(start));
			}
		}
		return operands;
	}

	private Expression parseAtom(int atomStart) {
		Expression expr;
		int pos = tokenPos;
		switch(currentToken) {
		case TOKEN_LP:
			nextToken();
			expr = parseBinary(atomStart);
			assertToken(TOKEN_RP);
			nextToken();
			break;
		case TOKEN_LB:
			nextToken();
			expr = factory.array(parseArray(pos), expression, atomStart, tokenPos - atomStart);
			assertToken(TOKEN_RB);
			nextToken();
			break;
		case TOKEN_LC:
			nextToken();
			expr = factory.hash(parseHash(pos), expression, atomStart, tokenPos - atomStart);
			assertToken(TOKEN_RC);
			nextToken();
			break;
		case TOKEN_LITERAL:
			expr = factory.constant(tokenValue, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_REGEXP:
			expr = factory.regexp((String)tokenValue, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_TYPE_NAME_SEGMENT: {
			atomStart = lastTokenPos;
			StringBuilder bld = null;
			String first = (String)tokenValue;
			int last = tokenPos;
			nextToken();
			while(currentToken == TOKEN_SEPARATOR) {
				nextToken();
				assertToken(TOKEN_TYPE_NAME_SEGMENT);
				if(bld == null) {
					bld = new StringBuilder();
					bld.append(first);
				}
				bld.append(OPERATOR_SEPARATOR);
				bld.append((String)tokenValue);
				nextToken();
				last = tokenPos;
			}
			expr = factory.typeName(bld == null ? first : bld.toString(), expression, atomStart, last - atomStart);
			break;
		}
		case TOKEN_IDENTIFIER_SEGMENT: {
			atomStart = lastTokenPos;
			StringBuilder bld = null;
			String first = (String)tokenValue;
			int last = tokenPos;
			nextToken();
			while(currentToken == TOKEN_SEPARATOR) {
				nextToken();
				assertToken(TOKEN_IDENTIFIER_SEGMENT);
				if(bld == null) {
					bld = new StringBuilder();
					bld.append(first);
				}
				bld.append(OPERATOR_SEPARATOR);
				bld.append((String)tokenValue);
				last = tokenPos;
				nextToken();
			}
			expr = factory.identifier(bld == null ? first : bld.toString(), expression, atomStart, last - atomStart);
			break;
		}
		case TOKEN_UNDEF:
			expr = factory.constant(null, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_TRUE:
			expr = factory.constant(Boolean.TRUE, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_FALSE:
			expr = factory.constant(Boolean.FALSE, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_DEFAULT:
			expr = factory.constant(Default.SINGLETON, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		default:
			throw syntaxError();
		}
		return expr;
	}

	private Expression parseBinary(int binaryStart) {
		Expression expr = parseUnary(binaryStart);
		if(currentToken == TOKEN_LB) {
			int pos = tokenPos;
			nextToken();
			expr = factory.access(expr, parseArray(pos), expression, binaryStart, tokenPos - binaryStart);
			assertToken(TOKEN_RB);
			nextToken();
		} else if(currentToken == TOKEN_ASSIGN) {
			int pos = tokenPos;
			nextToken();
			expr = factory.assignment(expr, parseBinary(pos), expression, binaryStart, tokenPos - binaryStart);
		}
		return expr;
	}

	private void parseDelimitedString(char delimiter, EscapeHandler escapeHandler) {
		int start = ++tokenPos;
		StringBuilder buf = new StringBuilder();
		int top = expression.length();
		while(tokenPos < top) {
			char ec = expression.charAt(tokenPos);
			if(ec == delimiter)
				break;
			if(ec == '\\') {
				if(++tokenPos == top)
					break;
				ec = expression.charAt(tokenPos);
				if(ec == delimiter) {
					buf.append(delimiter);
					++tokenPos;
				} else {
					tokenPos = escapeHandler.handle(buf, ++tokenPos, ec);
					if(tokenPos < 0) {
						tokenPos = start - 1;
						currentToken = TOKEN_ERROR;
						return;
					}
				}
				continue;
			}
			buf.append(ec);
			++tokenPos;
		}
		if(tokenPos == top) {
			tokenPos = start - 1;
			currentToken = TOKEN_ERROR;
		} else {
			++tokenPos;
			tokenValue = buf.toString();
			currentToken = TOKEN_LITERAL;
		}
	}

	private void parseDouble(int start, char d) {
		int top = expression.length();
		if(tokenPos + 1 >= top)
			// Must be room for the separator and at least one digit
			throw syntaxError();

		int segStart = ++tokenPos;
		findIntEnd();
		if(segStart == tokenPos)
			// No digits
			throw syntaxError();

		if(d == '.' && tokenPos + 1 < top) {
			// Check for 'e'
			char c = expression.charAt(tokenPos);
			if(c == 'e' || c == 'E') {
				segStart = ++tokenPos;
				findIntEnd();
				if(segStart == tokenPos)
					// No digits
					throw syntaxError();
			}
		}
		tokenValue = Double.valueOf(expression.substring(start, tokenPos));
		currentToken = TOKEN_LITERAL;
	}

	private void parseDoubleQuotedString() {
		parseDelimitedString('"', (buf, tokenPos, ec) -> {
			switch(ec) {
			case '\\':
			case '$':
			case '\'':
				buf.append(ec);
				break;
			case 'n':
				buf.append('\n');
				break;
			case 'r':
				buf.append('\r');
				break;
			case 't':
				buf.append('\t');
				break;
			case 's':
				buf.append(' ');
				break;
			case 'u':
				if(tokenPos + 4 >= expression.length())
					return -1;
				ec = expression.charAt(tokenPos);
				if(isHexDigit(ec)) {
					// Must be XXXX (a four-digit hex number)
					char[] digits = new char[]{ec, 0, 0, 0};
					for(int i = 1; i < 4; ++i) {
						char digit = expression.charAt(tokenPos + i);
						if(!isHexDigit(digit))
							return -1;
						digits[i] = digit;
					}
					buf.append(Character.toChars(Integer.parseInt(new String(digits), 16)));
					tokenPos += 4;
					break;
				}

				if(ec == '{') {
					// Must be {XXXXXX} (a hex number between two and six digits)
					StringBuilder digits = new StringBuilder();
					int top = expression.length();

					while(++tokenPos < top) {
						char digit = expression.charAt(tokenPos);
						if(isHexDigit(digit)) {
							digits.append(digit);
							continue;
						}

						if(digit == '}' && digits.length() >= 2 && digits.length() <= 6) {
							buf.append(Character.toChars(Integer.parseInt(digits.toString(), 16)));
							++tokenPos;
							break;
						}
						return -1;
					}
					break;
				}
				return -1;
			default:
				// Unrecognized escape sequence. Treat as literal backslash
				buf.append('\\');
				buf.append(ec);
			}
			return tokenPos;
		});
	}

	private List<Expression> parseHash(int start) {
		ArrayList<Expression> operands = new ArrayList<>();
		if(currentToken != TOKEN_RC) {
			for(; ; ) {
				operands.add(parseBinary(start));
				if(currentToken != TOKEN_ARROW)
					throw syntaxError();
				start = tokenPos;
				nextToken();
				operands.add(parseBinary(start));
				if(currentToken != TOKEN_COMMA)
					break;
				start = tokenPos;
				nextToken();
				if(currentToken == TOKEN_RC)
					// Extraneous comma causes this. That's OK
					break;
			}
		}
		return operands;
	}

	private void parseRegexpString() {
		parseDelimitedString('/', (buf, tokenPos, ec) -> {
			buf.append('\\');
			buf.append(ec);
			return tokenPos;
		});
	}

	private void parseSingleQuotedString() {
		parseDelimitedString('\'', (buf, tokenPos, ec) -> {
			buf.append('\\');
			if(ec != '\\')
				buf.append(ec);
			return tokenPos;
		});
	}

	private Expression parseUnary(int unaryStart) {
		if(currentToken == TOKEN_UNARY_MINUS) {
			int pos = tokenPos;
			nextToken();
			Expression expr = parseAtom(pos);
			return factory.negate(expr, expression, pos, tokenPos - pos);
		}
		return parseAtom(unaryStart);
	}

	private char skipWhite() {
		int top = expression.length();
		char c = 0;
		while(tokenPos < top) {
			c = expression.charAt(tokenPos);
			if(!Character.isWhitespace(c))
				break;
			++tokenPos;
		}
		return c;
	}

	private ParseException syntaxError() {
		Object tv = tokenValue;
		if(tv == null) {
			if(lastTokenPos >= expression.length())
				return syntaxError("Unexpected end of expression");
			tv = expression.substring(lastTokenPos, lastTokenPos + 1);
		}
		return syntaxError(format("Unexpected token '%s' at position %d in expression '%s'", tv, lastTokenPos,
				expression));
	}

	private ParseException syntaxError(String message) {
		return new ParseException(expression, message, tokenPos);
	}
}
