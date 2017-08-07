package com.puppet.pcore.impl.parser;

import com.puppet.pcore.Default;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.ParseException;

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
	private static final int TOKEN_DOT = 36;
	private static final int TOKEN_TYPE_NAME_SEGMENT = 40;
	private static final int TOKEN_IDENTIFIER_SEGMENT = 41;
	private static final int TOKEN_LITERAL = 42;
	private static final int TOKEN_LITERAL_HEREDOC = 43;
	private static final int TOKEN_DOLLAR = 44;
	private static final int TOKEN_VARIABLE = 45;
	private static final int TOKEN_REGEXP = 46;
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
	private String syntax;
	private int lastTokenPos;
	private int tokenPos;
	private int nextLineStart = -1; // Only set after parsing heredoc
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
		syntax = null;
		nextLineStart = -1;
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
		while(tokenPos < top && isDigit(expression.charAt(tokenPos)))
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
			++tokenPos;
			currentToken = TOKEN_ASSIGN;
			if(tokenPos < top) {
				switch(expression.charAt(tokenPos)) {
				case '>':
					tokenValue = OPERATOR_ARROW;
					currentToken = TOKEN_ARROW;
					++tokenPos;
				}
			}
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

		case '.':
			currentToken = TOKEN_DOT;
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
			currentToken = TOKEN_MINUS;
			++tokenPos;
			if(tokenPos < top) {
				c = expression.charAt(tokenPos);
				currentToken = c == '(' || isLetterOrDigit(c) ? TOKEN_UNARY_MINUS : TOKEN_MINUS;
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
				if(tokenPos + 1 >= top) {
					currentToken = TOKEN_ERROR;
					break;
				}
				int start = ++tokenPos;
				for(; tokenPos < top; ++tokenPos)
					if(!isHexDigit(expression.charAt(tokenPos)))
						break;
				if(tokenPos == start) {
					currentToken = TOKEN_ERROR;
					break;
				}
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

		case '$':
			currentToken = TOKEN_VARIABLE;
			tokenValue = (++tokenPos >= top) ? "" : parseVariable();
			break;

		case '@':
			parseHeredocString();
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

	private String parseVariable() {
		int start = tokenPos;
		int top = expression.length();
		if(tokenPos + 1 < top && expression.charAt(tokenPos) == ':' && expression.charAt(tokenPos + 1) == ':')
			tokenPos += 2;

		for(;;) {
			int segStart = tokenPos;
			while(tokenPos < top && isLetterOrDigit(expression.charAt(tokenPos)))
				++tokenPos;
			if(tokenPos == segStart)
				return "";

			if(tokenPos + 1 < top && expression.charAt(tokenPos) == ':' && expression.charAt(tokenPos + 1) == ':')
				continue;

			return expression.substring(start, tokenPos);
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
		case TOKEN_LITERAL_HEREDOC:
			expr = factory.heredoc(tokenValue, syntax, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_REGEXP:
			expr = factory.regexp((String)tokenValue, expression, lastTokenPos, tokenPos - lastTokenPos);
			nextToken();
			break;
		case TOKEN_VARIABLE:
			expr = factory.variable((String)tokenValue, expression, lastTokenPos, tokenPos - lastTokenPos);
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
			if(currentToken == TOKEN_TYPE_NAME_SEGMENT && first.equals("type")) {
				// "type TypeName" is special
				TypeNameExpression typeName = (TypeNameExpression)parseAtom(tokenPos);
				expr = factory.typeDeclaration(typeName.name, expression, atomStart, (typeName.offset - atomStart) + typeName.length);
			}
			else {
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
			}
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
		for(;;) {
			int pos = tokenPos;
			if(currentToken == TOKEN_LB) {
				nextToken();
				expr = factory.access(expr, parseArray(pos), expression, binaryStart, tokenPos - binaryStart);
				assertToken(TOKEN_RB);
				nextToken();
			} else if(currentToken == TOKEN_DOT) {
				nextToken();
				expr = factory.named_access(expr, parseUnary(pos), expression, binaryStart, tokenPos - binaryStart);
			} else if(currentToken == TOKEN_ASSIGN) {
				nextToken();
				expr = factory.assignment(expr, parseBinary(pos), expression, binaryStart, tokenPos - binaryStart);
			} else
				break;
		}
		return expr;
	}

	private void parseHeredocString() {
		int top = expression.length();
		if(++tokenPos >= top || expression.charAt(tokenPos) != '(') {
			currentToken = TOKEN_ERROR;
			return;
		}

		int escapeStart = -1;
		int quoteStart = -1;
		int syntaxStart = -1;
		String tag = null;
		char[] flags = null;
		int heredocTagEnd = -1;
		syntax = null;

		for(int start = ++tokenPos; tokenPos < top; ++tokenPos) {
			char ec = expression.charAt(tokenPos);

			switch(ec) {
			case ')':
				if(syntaxStart > 0)
					syntax = expression.substring(syntaxStart, tokenPos);
				if(escapeStart > 0) {
					flags = extractFlags(escapeStart, tokenPos);
					if(flags == null) {
						currentToken = TOKEN_ERROR;
						return;
					}
				}
				if(tag == null)
					tag = expression.substring(start, tokenPos);
				heredocTagEnd = tokenPos + 1;
				break;
			case '\n':
				currentToken = TOKEN_ERROR;
				return;
			case ':':
				if(syntaxStart > 0) {
					currentToken = TOKEN_ERROR;
					return;
				}
				if(tag == null)
					tag = expression.substring(start, tokenPos);
				syntaxStart = tokenPos + 1;
				continue;
			case '/':
				if(escapeStart > 0) {
					currentToken = TOKEN_ERROR;
					return;
				}
				if(tag == null)
					tag = expression.substring(start, tokenPos);
				else if(syntaxStart > 0) {
					syntax = expression.substring(syntaxStart, tokenPos);
					syntaxStart = -1;
				}
				escapeStart = tokenPos + 1;
				continue;
			case '"':
				if(tag != null) {
					currentToken = TOKEN_ERROR;
					return;
				}
				quoteStart = ++tokenPos;
				while(tokenPos < top) {
					char q = expression.charAt(tokenPos);
					if(q == '"')
						break;
					if(q == '\n') {
						currentToken = TOKEN_ERROR;
						return;
					}
					++tokenPos;
				}
				if(tokenPos == quoteStart) {
					currentToken = TOKEN_ERROR;
					return;
				}
				tag = expression.substring(quoteStart, tokenPos);
				continue;
			default:
				continue;
			}
			break;
		}

		if(tag == null || tag.isEmpty()) {
			currentToken = TOKEN_ERROR;
			return;
		}

		int heredocStart = -1;
		for(++tokenPos; tokenPos < top; ++tokenPos) {
			char c = expression.charAt(tokenPos);
			if(c == '#')
				c = skipWhite(true);
			else if(c == '/' && tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '*') {
				--tokenPos;
				c = skipWhite(true);
			}

			if(c == '\n') {
				if(nextLineStart >= 0) {
					tokenPos = nextLineStart;
					nextLineStart = -1;
				} else
					++tokenPos;
				heredocStart = tokenPos;
				break;
			}
		}

		if(heredocStart == -1) {
			currentToken = TOKEN_ERROR;
			return;
		}

		boolean suppressLastNL = false;
		int heredocContentEnd = -1;
		int heredocEnd = -1;
		int indentStrip = 0;
		int tagLen = tag.length();

		for(; tokenPos < top; ++tokenPos) {
			char ec = expression.charAt(tokenPos);
			if(ec == '\n' && ++tokenPos < top) {
				int lineStart = tokenPos;
				skipWhite(true);
				if(tokenPos >= top)
					break;
				ec = expression.charAt(tokenPos);
				if(ec == '|') {
					indentStrip = tokenPos - lineStart;
					++tokenPos;
					skipWhite(true);
					if(tokenPos >= top)
						break;
					ec = expression.charAt(tokenPos);
				}
				if(ec == '-') {
					++tokenPos;
					suppressLastNL = true;
					skipWhite(true);
					if(tokenPos >= top)
						break;
					ec = expression.charAt(tokenPos);
				}
				if(ec == '\n') {
					--tokenPos;
					continue;
				}
				if(!(tokenPos + tagLen <= top && expression.substring(tokenPos, tokenPos + tagLen).equals(tag)))
					continue;

				tokenPos += tagLen;
				while(tokenPos < top) {
					ec = expression.charAt(tokenPos);
					if(ec == '\n')
						break;
					if(!Character.isWhitespace(ec))
						break;
					++tokenPos;
				}

				if(ec == '\n' || tokenPos == top) {
					heredocContentEnd = lineStart;
					if(suppressLastNL) {
						--heredocContentEnd;
						if(expression.charAt(heredocContentEnd - 1) == '\r')
							--heredocContentEnd;
					}
					heredocEnd = tokenPos + 1;
					break;
				}
			}
		}

		if(heredocContentEnd == -1) {
			currentToken = TOKEN_ERROR;
			return;
		}

		currentToken = TOKEN_LITERAL_HEREDOC;
		tokenPos = heredocTagEnd;
		nextLineStart = heredocEnd;
		String heredoc = expression.substring(heredocStart, heredocContentEnd);
		if(flags != null)
			heredoc = applyEscapes(heredoc, flags);
		if(indentStrip > 0)
			heredoc = Helpers.unindent(heredoc, indentStrip);
		tokenValue = heredoc;
	}

	private char[] extractFlags(int start, int end) {
		int top = end - start;
		char[] flags = new char[top];
		for(int idx = 0; idx < top; ++idx) {
			char flag = expression.charAt(start++);
			switch(flag) {
			case 't':case 'r':case 'n':case 's':case '$':
				flags[idx] = flag;
				break;
			case 'L':
				flags[idx] = '\n';
				break;
			default:
				return null;
			}
		}
		return flags;
	}

	private String applyEscapes(String str, char[] flags) {
		StringBuilder bld = new StringBuilder();
		int top = str.length();
		for(int idx = 0; idx < top; ++idx) {
			char c = str.charAt(idx);
			if(c == '\\' && idx + 1 < top) {
				c = str.charAt(++idx);
				boolean escaped = false;
				int fi = flags.length;
				while(--fi >= 0) {
					if(flags[fi] == c) {
						escaped = true;
						break;
					}
				}
				if(!escaped) {
					bld.append('\\');
					bld.append(c);
					continue;
				}

				switch(c) {
				case 'r':
					bld.append('\r');
					break;
				case 'n':
					bld.append('\n');
					break;
				case 't':
					bld.append('\t');
					break;
				case 's':
					bld.append(' ');
					break;
				case 'u':
					idx = appendUnicode(str, idx, bld);
					break;
				case '\n':
					break;
				default:
					bld.append(c);
				}
			} else
				bld.append(c);
		}
		return bld.toString();
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
				tokenPos = appendUnicode(expression, tokenPos, buf);
				break;
			default:
				// Unrecognized escape sequence. Treat as literal backslash
				buf.append('\\');
				buf.append(ec);
			}
			return tokenPos;
		});
	}

	private static int appendUnicode(String expression, int tokenPos, StringBuilder buf) {
		if(tokenPos + 4 >= expression.length())
			return -1;
		char ec = expression.charAt(tokenPos);
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
			return tokenPos + 4;
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
			return tokenPos;
		}
		return -1;
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
		return skipWhite(false);
	}

	private char skipWhite(boolean breakOnNewline) {
		char commentStart = 0;
		int top = expression.length();
		while(tokenPos < top) {
			char c = expression.charAt(tokenPos);
			switch(c) {
			case '\n':
				if(commentStart == '*')
					break;

				if(breakOnNewline)
					return c;

				if(nextLineStart >= 0) {
					tokenPos = nextLineStart - 1;
					nextLineStart = -1;
				}
				if(commentStart == '#')
					commentStart = 0;
				break;

			case '#':
				if(commentStart == 0)
					commentStart = '#';
				break;

			case '/':
				if(commentStart == 0) {
					if(tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '*') {
						++tokenPos;
						commentStart = '*';
						break;
					}
				}
				return c;

			case '*':
				if(commentStart == '*' && tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '/') {
					++tokenPos;
					commentStart = 0;
					break;
				}
				return c;

			case ' ':
			case '\r':
			case '\t':
				break;

			default:
				if(commentStart == 0)
					return c;
				break;
			}
			++tokenPos;
		}
		return 0;
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
