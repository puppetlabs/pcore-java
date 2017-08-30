package com.puppet.pcore.impl.parser;

import com.puppet.pcore.Default;
import com.puppet.pcore.IssueException;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.ParseException;
import com.puppet.pcore.parser.ParseIssue;
import com.puppet.pcore.parser.model.*;

import java.util.ArrayList;
import java.util.List;

import static com.puppet.pcore.parser.ParseIssue.*;
import static com.puppet.pcore.impl.parser.LexTokens.*;
import static java.lang.String.format;

public class Lexer extends StringReader {

	private final boolean handleBacktickStrings;

	public Lexer(boolean handleBacktickStrings) {
		this.handleBacktickStrings = handleBacktickStrings;
	}

	@FunctionalInterface
	private interface EscapeHandler {
		void handle(StringBuilder buf, char quoteChar);
	}

	boolean canParse() {
		return false;
	}

	Expression parse(int expectedEnd, boolean singleExpression) {
		// Must be subclassed by parser in order to handle interpolations
		throw new UnsupportedOperationException("interpolation not supported by lexer");
	}

	int currentToken;
	Locator locator;
	int tokenStartPos;
	int radix;
	Object tokenValue;

	private int beginningOfLine;
	private int nextLineStart = -1; // Only set after parsing heredoc
	private boolean eppMode;

	final void init(String file, String exprString, boolean eppMode) {
		reset(exprString);
		locator = new Locator(file, exprString);
		currentToken = 0;
		tokenValue = null;
		radix = 10;
		beginningOfLine = 0;
		nextLineStart = -1;
		this.eppMode = eppMode;
	}

	final IssueException parseIssue(ParseIssue issueCode, Object...args) {
		return new IssueException(issueCode, args, new ParseLocation(locator, pos()));
	}

	final String tokenString() {
		if(tokenValue == null)
			return tokenMap.get(currentToken);
		if(tokenValue instanceof String)
			return (String)tokenValue;
		throw new ParseException(format("Token '%s' has no string representation", tokenMap.get(currentToken)));
	}

	static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	static boolean isHexDigit(char c) {
		return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
	}

	static boolean isLetter(char c) {
		return isLowercaseLetter(c) || isUppercaseLetter(c);
	}

	static boolean isLetterOrDigit(char c) {
		return isLetter(c) || isDigit(c) || c == '_';
	}

	static boolean isLowercaseLetter(char c) {
		return c >= 'a' && c <= 'z';
	}

	static boolean isOctalDigit(char c) {
		return c >= '0' && c <= '7';
	}

	static boolean isUppercaseLetter(char c) {
		return c >= 'A' && c <= 'Z';
	}

	void nextToken() {
		int scanStart = pos();
		char c = skipWhite();
		if(c == 0) {
			setToken(TOKEN_END);
			return;
		}

		int start = pos() - 1; // start of c
		tokenStartPos = start;

		if('1' <= c && c <= '9') {
			skipDecimalDigits();
			c = peek();
			if(c == '.' || c == 'e' || c == 'E') {
				advance();
				consumeFloat(start, c);
			} else {
				if(Character.isLetter(c))
					throw parseIssue(LEX_DIGIT_EXPECTED);
				setToken(TOKEN_INTEGER, Long.parseLong(from(start), 10));
				radix = 10;
			}
			return;
		}

		if('A' <= c && c <= 'Z') {
			consumeQualifiedName(start, TOKEN_TYPE_NAME);
			return;
		}

		if('a' <= c && c <= 'z') {
			consumeQualifiedName(start, TOKEN_IDENTIFIER);
			return;
		}

		switch(c) {
		case '=':
			switch(peek()) {
			case '=':
				advance();
				setToken(TOKEN_EQUAL);
				break;
			case '~':
				advance();
				setToken(TOKEN_MATCH);
				break;
			case '>':
				advance();
				setToken(TOKEN_FARROW);
				break;
			default:
				setToken(TOKEN_ASSIGN);
			}
			break;

		case '{':
			setToken(currentToken == TOKEN_QMARK ? TOKEN_SELC : TOKEN_LC);
			break;

		case '}':
			setToken(TOKEN_RC);
			break;

		case '[':
			// If token is preceded by whitespace or if it's the first token to be parsed, then it's a
			// list rather than parameters to an access expression
			setToken(scanStart < start || start == 0 ? TOKEN_LISTSTART : TOKEN_LB);
			break;

		case ']':
			setToken(TOKEN_RB);
			break;

		case '(':
			// If token is first on line or only preceded by whitespace, then it is not start of parameters
			// in a call.
		{
			int savePos = pos();
			setPos(beginningOfLine);
			skipWhite();
			int firstNonWhite = pos() - 1;
			setPos(savePos);
			setToken(firstNonWhite == start ? TOKEN_WSLP : TOKEN_LP);
			break;
		}

		case ')':
			setToken(TOKEN_RP);
			break;

		case ',':
			setToken(TOKEN_COMMA);
			break;

		case ';':
			setToken(TOKEN_SEMICOLON);
			break;

		case '.':
			setToken(TOKEN_DOT);
			break;

		case '?':
			setToken(TOKEN_QMARK);
			break;

		case ':':
			setToken(TOKEN_COLON);
			c = peek();
			if(c == ':') {
				advance();
				c = next();
				if(isUppercaseLetter(c))
					consumeQualifiedName(start, TOKEN_TYPE_NAME);
				else if(isLowercaseLetter(c))
					consumeQualifiedName(start, TOKEN_IDENTIFIER);
				else {
					setPos(start);
					throw parseIssue(LEX_DOUBLE_COLON_NOT_FOLLOWED_BY_NAME);
				}
			}
			break;

		case '-':
			switch(peek()) {
			case '=':
				advance();
				setToken(TOKEN_SUBTRACT_ASSIGN);
				break;
			case '>':
				advance();
				setToken(TOKEN_IN_EDGE);
				break;
			case '%':
				if(eppMode) {
					advance();
					c = peek();
					if(c == '>') {
						advance();
						for(c = peek(); c == ' ' || c == '\t'; c = peek())
							advance();
						if(c == '\n')
							advance();
						consumeEPP();
					} else
						throw parseIssue(LEX_INVALID_OPERATOR, "-%");
					break;
				}
				/* fallthrough */

			default:
				setToken(TOKEN_SUBTRACT);
			}
			break;

		case '+':
			c = peek();
			if(c == '=') {
				advance();
				setToken(TOKEN_ADD_ASSIGN);
			} else if(c == '>') {
				advance();
				setToken(TOKEN_PARROW);
			} else
				setToken(TOKEN_ADD);
			break;

		case '*':
			setToken(TOKEN_MULTIPLY);
			break;

		case '%':
			setToken(TOKEN_REMAINDER);
			if(eppMode) {
				c = peek();
				if(c == '>') {
					advance();
					consumeEPP();
				}
			}
			break;

		case '!':
			c = peek();
			if(c == '=') {
				advance();
				setToken(TOKEN_NOT_EQUAL);
			} else if(c == '~') {
				advance();
				setToken(TOKEN_NOT_MATCH);
			} else
				setToken(TOKEN_NOT);
			break;

		case '>':
			c = peek();
			if(c == '=') {
				advance();
				setToken(TOKEN_GREATER_EQUAL);
			} else if(c == '>') {
				advance();
				setToken(TOKEN_RSHIFT);
			} else
				setToken(TOKEN_GREATER);
			break;

		case '~':
			c = peek();
			if(c == '>') {
				advance();
				setToken(TOKEN_IN_EDGE_SUB);
			} else {
				// Standalone tilde is not an operator in Puppet
				setPos(start);
				throw parseIssue(LEX_UNEXPECTED_TOKEN, "~");
			}
			break;

		case '@':
			c = peek();
				if(c == '@') {
				advance();
				setToken(TOKEN_ATAT);
			} else if(c == '(') {
				advance();
				consumeHeredocString();
			} else
				setToken(TOKEN_AT);
			break;

		case '<':
			switch(peek()) {
			case '=':
				advance();
				setToken(TOKEN_LESS_EQUAL);
				break;
			case '<':
				advance();
				c = peek();
				if(c == '|') {
					advance();
					setToken(TOKEN_LLCOLLECT);
				} else
					setToken(TOKEN_LSHIFT);
				break;
			case '|':
				advance();
				setToken(TOKEN_LCOLLECT);
				break;
			case '-':
				advance();
				setToken(TOKEN_OUT_EDGE);
				break;
			case '~':
				advance();
				setToken(TOKEN_OUT_EDGE_SUB);
				break;
			case '%':
				if (eppMode) {
					advance();
					// <%# and <%% has been dealt with in consumeEPP so there's no need to deal with
					// that. Only <%, <%- and <%= can show up here
					switch(peek()) {
						case '=':
							advance();
							setToken(TOKEN_RENDER_EXPR);
							break;
						case '-':
							advance();
							nextToken();
							break;
						default:
							nextToken();
					}
					break;
				}
				// fallthrough
			default:
				setToken(TOKEN_LESS);
			}
			break;

		case '|':
			switch(peek()) {
			case '>':
				advance();
				c = peek();
				if(c == '>') {
					advance();
					setToken(TOKEN_RRCOLLECT);
				} else
					setToken(TOKEN_RCOLLECT);
				break;
			default:
				setToken(TOKEN_PIPE);
			}
			break;

		case '"':
			consumeDoubleQuotedString();
			break;

		case '\'':
			consumeSingleQuotedString();
			break;

		case '/':
			if(!(isRegexpAcceptable() && consumeRegexp()))
				setToken(TOKEN_DIVIDE);
			break;

    case '$':
      c = peek();
      if(c == ':' ) {
      	advance();
	      c = peek();
        if(c != ':' ) {
          setPos(start);
          throw parseIssue(LEX_INVALID_VARIABLE_NAME);
        }
        advance();
	      c = peek();
      }
      if(isLowercaseLetter(c)) {
	      advance();
	      consumeQualifiedName(start, TOKEN_VARIABLE);
      } else if(isDigit(c)) {
	      advance();
        skipDecimalDigits();
        setToken(TOKEN_VARIABLE, from(start + 1));
      } else if(Character.isLetter(c)) {
	      setPos(start);
	      throw parseIssue(LEX_INVALID_VARIABLE_NAME);
      } else
	      setToken(TOKEN_VARIABLE, "");
      break;

    case '0':
      radix = 10;
      c = peek();
      switch(c) {
      case 0:
	      setToken(TOKEN_INTEGER, 0L);
	      break;

      case 'x':
      case 'X': {
	      advance(); // consume 'x'
	      int hexStart = pos();
	      c = peek();
	      while(isHexDigit(c)) {
		      advance();
		      c = peek();
	      }
	      if(pos() == hexStart || isLetter(c))
		      throw parseIssue(LEX_HEXDIGIT_EXPECTED);
	      radix = 16;
	      setToken(TOKEN_INTEGER, Long.valueOf(from(hexStart), 16));
	      break;
      }
      case '.':
      case 'e':
      case 'E':
	      // 0[.eE]<something>
	      advance();
	      consumeFloat(start, c);
	      break;

      default: {
	      int octalStart = pos();
	      while(isOctalDigit(c)) {
		      advance();
		      c = peek();
	      }
	      if(isDigit(c) || Character.isLetter(c))
		      throw parseIssue(LEX_OCTALDIGIT_EXPECTED);
	      if(pos() > octalStart) {
		      radix = 8;
		      setToken(TOKEN_INTEGER, Long.valueOf(from(octalStart), 8));
	      } else {
		      setToken(TOKEN_INTEGER, 0L);
	      }
	      break;
      }
      }
      break;

		case '`':
			if(handleBacktickStrings) {
				consumeBacktickedString();
				break;
			}
			// Fall through
    default:
      setPos(start);
      throw parseIssue(LEX_UNEXPECTED_TOKEN, new String(new char[] {c}));
		}
	}

	private void setToken(int token) {
		currentToken = token;
		tokenValue = null;
	}

	private void setToken(int token, Object value) {
		currentToken = token;
		tokenValue = value;
	}

	private int skipDecimalDigits() {
		int digitCount = 0;
		char c = peek();
		if(c == '-' || c == '+') {
			advance();
			c = peek();
		}
		while(isDigit(c)) {
			advance();
			c = peek();
			digitCount++;
		}
		return digitCount;
	}

	private class HeredocStart {
		final String tag;
		final boolean interpolate;
		final String syntax;
		final char[] flags;
		final int heredocTagEnd;

		HeredocStart(String tag, boolean interpolate, String syntax, char[] flags, int heredocTagEnd) {
			this.tag = tag;
			this.interpolate = interpolate;
			this.syntax = syntax;
			this.flags = flags;
			this.heredocTagEnd = heredocTagEnd;
		}
	}

	private HeredocStart parseHeredocInfo(int heredocStart) {
		char[] flags = null;
		String tag = null;
		String syntax = null;

		int escapeStart = -1;
		int quoteStart = -1;
		int syntaxStart = -1;

		final int heredocTagEnd;

		findTagEnd: for(final int start = pos();;) {
			char c = peek();
			switch(c) {
			case 0:
			case '\n':
				setPos(heredocStart);
				throw parseIssue(LEX_HEREDOC_DECL_UNTERMINATED);

			case ')':
				if(syntaxStart > 0 ) {
					syntax = from(syntaxStart);
				}
				if(escapeStart > 0 ) {
					flags = extractFlags(escapeStart);
				}
				if(tag == null)
					tag = from(start);

				advance();
				heredocTagEnd = pos();
				break findTagEnd;

			case ':':
				if(syntaxStart > 0)
					throw parseIssue(LEX_HEREDOC_MULTIPLE_SYNTAX);

				if(tag == null)
					tag = from(start);

				advance();
				syntaxStart = pos();
				break;

			case '/':
				if(escapeStart > 0 )
					throw parseIssue(LEX_HEREDOC_MULTIPLE_ESCAPE);

				if(tag == null) {
					tag = from(start);
				} else if(syntaxStart > 0) {
					syntax = from(syntaxStart);
					syntaxStart = -1;
				}
				advance();
				escapeStart = pos();
				break;

			case '"':
				if(tag != null)
					throw parseIssue(LEX_HEREDOC_MULTIPLE_TAG);
				advance();
				quoteStart = pos();
				findEndQuote: for(;;) {
					c = peek();
					switch(c) {
					case 0: case '\n':
						setPos(heredocStart);
						throw parseIssue(LEX_HEREDOC_DECL_UNTERMINATED);
					case '"':
						break findEndQuote;
					default:
						advance();
					}
				}
				if(quoteStart == pos()) {
					setPos(heredocStart);
					throw parseIssue(LEX_HEREDOC_EMPTY_TAG);
				}
				tag = from(quoteStart);
				advance();
				break;
			default:
				advance();
			}
		}

		if(tag == null || tag.isEmpty()) {
			setPos(heredocStart);
			throw parseIssue(LEX_HEREDOC_EMPTY_TAG);
		}
		return new HeredocStart(tag, quoteStart >= 0, syntax, flags, heredocTagEnd);
	}

	private void consumeUntilNextLine(int heredocStart) {
		for(char c = peek();;) {
			switch(c) {
			case 0:
				setPos(heredocStart);
				throw parseIssue(LEX_HEREDOC_UNTERMINATED);

			case '#':
				c = skipWhite(true);
				break;

			case '/': {
				int n = pos();
				advance();
				c = next();
				if(c == '*') {
					setPos(n); // rewind to comment start
					c = skipWhite(true); // skip comment
				}
				break;
			}

			case '\n':
				if(nextLineStart >= 0 ) {
					setPos(nextLineStart);
					nextLineStart = -1;
				} else {
					advance();
				}
				return;

			default:
				advance();
				c = peek();
			}
		}
	}

	private class HeredocEnd {
		final int indentStrip;
		final int heredocContentEnd;
		final int heredocEnd;

		HeredocEnd(int indentStrip, int heredocContentEnd, int heredocEnd) {
			this.indentStrip = indentStrip;
			this.heredocContentEnd = heredocContentEnd;
			this.heredocEnd = heredocEnd;
		}
	}

	private HeredocEnd consumeUntilEndTag(int heredocStart, HeredocStart info) {
		// Find end of heredoc and heredoc content
		int indentStrip = 0;
		int heredocContentEnd;
		int heredocEnd;

		boolean suppressLastNL = false;
		final int tagLen = info.tag.length();

		final char tagStart = info.tag.charAt(0);
		findEndOfText: for (char c = next();;) {
			switch(c) {
			case 0:
				setPos(heredocStart);
				throw parseIssue(LEX_HEREDOC_UNTERMINATED);

			case '\n':
				int lineStart = pos();
				c = skipWhiteInLiteral();
				switch(c) {
				case 0:
					setPos(heredocStart);
					throw parseIssue(LEX_HEREDOC_UNTERMINATED);

				case '|':
					indentStrip = (pos() - 1) - lineStart;
					c = skipWhiteInLiteral();
					if(c != '-')
						break;

					// fallthrough
				case '-':
					suppressLastNL = true;
					c = skipWhiteInLiteral();
				}

				if(c != tagStart)
					continue;

				String expr = locator.source;
				int tagStartPos = pos() - 1;
				int tagEndPos = tagStartPos + tagLen;
				if(tagEndPos <= expr.length() && info.tag.equals(expr.substring(tagStartPos, tagEndPos))) {
					// tag found if rest of line is whitespace
					setPos(tagEndPos);
					c = skipWhiteInLiteral();
					heredocEnd = pos() - 1;
					if(c == '\n' || c == 0) {
						heredocContentEnd = lineStart;
						if(suppressLastNL) {
							heredocContentEnd--;
							if(expr.charAt(heredocContentEnd - 1) == '\r')
								heredocContentEnd--;
						}
						break findEndOfText;
					}
				}
				break;

			default:
				c = next();
			}
		}
		return new HeredocEnd(indentStrip, heredocContentEnd, heredocEnd);
	}

	private void consumeHeredocString() {
		final int heredocStart = pos() - 2; // Backtrack '@' and '('

		// Parse the tag with syntax and flags
		HeredocStart start = parseHeredocInfo(heredocStart);

		// Find where actual text starts
		consumeUntilNextLine(heredocStart);
		int heredocContentStart = pos();

		// Get the heredoc text and info provided by the end tag
		HeredocEnd end = consumeUntilEndTag(heredocStart, start);

		String heredoc;
		if(start.flags != null || start.interpolate || end.indentStrip > 0) {
			setPos(heredocContentStart);
			List<Expression> segments = null;
			if(start.interpolate && canParse())
				segments = new ArrayList<>();

			heredoc = applyEscapes(end.heredocContentEnd, end.indentStrip, start.flags, segments);
			if(segments != null && segments.size() > 0) {
				if(heredoc.length() > 0)
					segments.add(new LiteralString(heredoc, locator, tokenStartPos, pos() - tokenStartPos));

				setPos(start.heredocTagEnd); // Normal parsing continues here
				nextLineStart = end.heredocEnd + 1; // and next newline will jump to here
				Expression textExpr = new ConcatenatedString(segments, locator, heredocContentStart, end.heredocContentEnd -heredocContentStart);
				setToken(TOKEN_HEREDOC, new HeredocExpression(textExpr, start.syntax, locator, heredocStart, end.heredocContentEnd - heredocStart));
				return;
			}
		} else {
			setPos(end.heredocContentEnd);
			heredoc = from(heredocContentStart);
		}

		setPos(start.heredocTagEnd); // Normal parsing continues here
		nextLineStart = end.heredocEnd + 1; // and next newline will jump to here
		if(canParse()) {
			Expression textExpr = new LiteralString(heredoc, locator, heredocContentStart, end.heredocContentEnd - heredocContentStart);
			setToken(TOKEN_HEREDOC, new HeredocExpression(textExpr, start.syntax, locator, heredocStart, end.heredocContentEnd - heredocStart));
		} else
			setToken(TOKEN_STRING, heredoc);
	}

	private char skipWhiteInLiteral() {
		for(char c = next();; c = next()) {
			if(!(c == ' ' || c == '\r' || c == '\t'))
				return c;
		}
	}

	private char[] extractFlags(int start) {
		String s = from(start);
		int top = s.length();
		char[] flags = new char[top];
		for(int idx = 0; idx < top; ++idx) {
			char flag = s.charAt(idx);
			switch(flag) {
			case 't': case 'r': case 'n': case 's': case 'u': case '$':
					flags[idx] = flag;
					break;
				case 'L':
					flags[idx] = '\n';
					break;
				default:
					setPos(start);
					throw parseIssue(LEX_HEREDOC_ILLEGAL_ESCAPE, new String(new char[] { flag }));
			}
		}
		return flags;
	}

	private String applyEscapes(int end, int indentStrip, char[] flags, List<Expression> segments) {
		StringBuilder bld = new StringBuilder();
		stripIndent(indentStrip);
		int start = pos();
		for(char c = next(); c != 0 && start < end; start = pos(), c = next()) {
			if(c != '\\') {
				if(c == '$' && segments != null) {
					handleInterpolation(start, segments, bld);
				} else {
					bld.append(c);
					if(c == '\n')
						stripIndent(indentStrip);
				}
				continue;
			}

			start = pos();
			c = next();
			if(start >= end) {
				bld.append('\\');
				break;
			}

			boolean escaped = false;
			int fi = flags == null ? 0 : flags.length;
			while(--fi >= 0) {
				if(flags[fi] == c) {
					escaped = true;
					break;
				}
			}
			if(!escaped) {
				bld.append('\\');
				if(c == '$' && segments != null)
					handleInterpolation(start, segments, bld);
				else {
					bld.append(c);
					if(c == '\n')
						stripIndent(indentStrip);
				}
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
					appendUnicode(bld);
					break;
				case '\n':
					stripIndent(indentStrip);
					break;
				default:
					bld.append(c);
			}
		}
		return bld.toString();
	}

	private void stripIndent(int indentStrip) {
		int start = pos();
		while(indentStrip > 0) {
			char c = peek();
			if(c == '\t' || c == ' ') {
				advance();
				indentStrip--;
				continue;
			}
			// Lines that cannot have their indent stripped i full, does not
			// get it stripped at all
			setPos(start);
			break;
		}
	}

	private void consumeDelimitedString(char delimiter, List<Expression> segments, EscapeHandler handler) {
		int start = pos();
		StringBuilder buf = new StringBuilder();
		char ec = next();
		for(;;) {
			switch(ec) {
			case 0:
				if(delimiter != '/')
					throw unterminatedQuote(start - 1, delimiter);

				setToken(TOKEN_DIVIDE);
				return;

			case '\n':
				if(delimiter != '/')
				  throw unterminatedQuote(start - 1, delimiter);

				buf.append(ec);
				ec = next();
				break;

			case '\\':
				ec = next();
				if(ec == 0)
					throw unterminatedQuote(start - 1, delimiter);

				if(ec == delimiter) {
					buf.append(delimiter);
					ec = next();
					continue;
				}

				handler.handle(buf, ec);
				ec = next();
				continue;

			case '$':
				if(segments != null) {
					handleInterpolation(start, segments, buf);
					ec = next();
					continue;
				}

				// treat '$' just like any other character when segments is nil
				// fallthrough
			default:
				if(ec == delimiter) {
					setToken(TOKEN_STRING, buf.toString());
					return;
				}
				buf.append(ec);
				ec = next();
			}
		}
	}

	private IssueException unterminatedQuote(int start, char delimiter) {
		setPos(start);
		return parseIssue(LEX_UNTERMINATED_STRING, delimiter == '"' ? "double" : "single");
	}

	private void consumeFloat(int start, char d) {
		if(skipDecimalDigits() == 0)
			throw parseIssue(LEX_DIGIT_EXPECTED);

		char c = peek();
		if(d == '.') {
			// Check for 'e'
			if(c == 'e' || c == 'E') {
				advance();
				if(skipDecimalDigits() == 0)
					throw parseIssue(LEX_DIGIT_EXPECTED);
				c = peek();
			}
		}
		if(Character.isLetter(c))
			throw parseIssue(LEX_DIGIT_EXPECTED);

		setToken(TOKEN_FLOAT, Double.valueOf(from(start)));
	}

	private void consumeQualifiedName(int start, int token) {
		boolean lastStartsWithUnderscore = false;
		for(;;) {
			char c = peek();
			while(isLetterOrDigit(c)) {
				advance();
				c = peek();
			}

			if(c != ':')
				break;

			int nameEnd = pos();
			advance();
			c = peek();

			if(c != ':') {
				// Single ':' after a name is ok. Should not be consumed
				setPos(nameEnd);
				break;
			}

			advance();
			c = peek();
			if(token == TOKEN_TYPE_NAME && isUppercaseLetter(c) ||
					token != TOKEN_TYPE_NAME && (isLowercaseLetter(c) || token == TOKEN_VARIABLE && c == '_')) {
				if(!lastStartsWithUnderscore) {
					advance();
					lastStartsWithUnderscore = c == '_';
					continue;
				}
			}

			setPos(start);
			ParseIssue issueCode;
			if(token == TOKEN_TYPE_NAME)
				issueCode = LEX_INVALID_TYPE_NAME;
			else if(token == TOKEN_VARIABLE)
				issueCode = LEX_INVALID_VARIABLE_NAME;
			else
				issueCode = LEX_INVALID_NAME;
			throw parseIssue(issueCode);
		}

		if(token == TOKEN_VARIABLE)
			++start; // skip leading '$´

		String word = from(start);
		setToken(token, word);

		if(token == TOKEN_IDENTIFIER) {
			Integer kwToken = keywords.get(word);
			if(kwToken != null) {
				switch(word) {
	      case "true":
					setToken(kwToken, true);
					break;
	      case "false":
					setToken(kwToken, false);
					break;
	      case "default":
					setToken(kwToken, Default.SINGLETON);
					break;
				default:
					setToken(kwToken, word);
				}
			}
		}
	}

	private void consumeDoubleQuotedString() {
		List<Expression> segments = null;
		if(canParse())
			segments = new ArrayList<>();
		consumeDelimitedString('"', segments, (buf, ec) -> {
			switch(ec) {
			case '\\':
			case '\'':
				buf.append(ec);
				break;
			case '$':
				if(!canParse())
					buf.append('\\');
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
				appendUnicode(buf);
				break;
			default:
				// Unrecognized escape sequence. Treat as literal backslash
				buf.append('\\');
				buf.append(ec);
			}
		});
		if(!canParse())
			// currentToken will be TOKEN_STRING
			return;

		if(segments.size() > 0) {
			if(currentToken == TOKEN_STRING) {
				String tail = tokenString();
				if(!tail.isEmpty()) {
					segments.add(new LiteralString(tail, locator, tokenStartPos, pos() - tokenStartPos));
				}
			}
		} else
			segments.add(new LiteralString(tokenString(), locator, tokenStartPos, pos() - tokenStartPos));

		int firstPos = segments.get(0).offset();
		setToken(TOKEN_CONCATENATED_STRING, new ConcatenatedString(segments, locator, firstPos, pos() - firstPos));
	}

	private void appendUnicode(StringBuilder buf) {
		int start = pos();
		char ec = next();
		if(isHexDigit(ec)) {
			// Must be XXXX (a four-digit hex number)
			for(int i = 1; i < 4; ++i) {
				char digit = next();
				if(!isHexDigit(digit)) {
					setPos(start - 2);
					throw parseIssue(LEX_MALFORMED_UNICODE_ESCAPE);
				}
			}
			int c = Integer.parseInt(from(start), 16);
			buf.append(Character.toChars(c));
			return;
		}

		if(ec != '{') {
			setPos(start - 2);
			throw parseIssue(LEX_MALFORMED_UNICODE_ESCAPE);
		}

		// Must be {XXxxxx} (a hex number between two and six digits
		int hexStart = pos();
		ec = peek();
		while(isHexDigit(ec)) {
			advance();
			ec = peek();
		}
		int uLen = pos() - hexStart;
		if(!(uLen >= 2 && uLen <= 6 && ec == '}')) {
			setPos(start - 2);
			throw parseIssue(LEX_MALFORMED_UNICODE_ESCAPE);
		}
		int c = Integer.parseInt(from(hexStart), 16);
		buf.append(Character.toChars(c));
		advance(); // Skip terminating '}'
	}

	private boolean consumeRegexp() {
		int start = pos();
		consumeDelimitedString('/', null, (buf, ec) -> {
			buf.append('\\');
			buf.append(ec);
		});
		if(currentToken == TOKEN_STRING) {
			currentToken = TOKEN_REGEXP;
			return true;
		}
		setPos(start);
		return false;
	}

	private void consumeBacktickedString() {
		int start = pos();
		if(!find('`'))
			throw parseIssue(LEX_UNTERMINATED_STRING, "backtick");
		setToken(TOKEN_STRING, from(start));
		advance(); // skip backtick
	}

	private void consumeSingleQuotedString() {
		consumeDelimitedString('\'', null, (buf, ec) -> {
			buf.append('\\');
			if(ec != '\\')
				buf.append(ec);
		});
	}

	private char skipWhite() {
		return skipWhite(false);
	}

	private char skipWhite(boolean breakOnNewline) {
		char commentStart = 0;
		int commentStartPos = 0;
		for(;;) {
			int start = pos();
			char c = next();
			switch(c) {
			case 0:
				if(commentStart == '*') {
					setPos(commentStartPos);
					throw parseIssue(LEX_UNTERMINATED_COMMENT);
				}
				return 0;

			case '\n':
				if(commentStart == '*')
					break;

				if(breakOnNewline) {
					setPos(start);
					return c;
				}

				if(nextLineStart >= 0) {
					setPos(nextLineStart);
					nextLineStart = -1;
				}
				if(commentStart == '#')
					commentStart = 0;
				beginningOfLine = pos();
				break;

			case '#':
				if(commentStart == 0) {
					commentStartPos = start;
					commentStart = '#';
				}
				break;

			case '/':
				if(commentStart == 0) {
					if(peek() == '*') {
						advance();
						commentStart = '*';
						commentStartPos = start;
						break;
					}
					return c;
				}
				break;

			case '*':
				if(commentStart == '#')
					continue;
				if(commentStart == '*' && peek() == '/') {
					advance();
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
			}
		}
	}

	void consumeEPP() {
		StringBuilder buf = new StringBuilder();
		int lastNonWS = 0;
		int start = pos();
		for(char ec = next(); ec != 0; start = pos(), ec = next()) {
			switch(ec ) {
			case '<':
				ec= peek();
				if(ec != '%' ) {
					buf.append('<');
					lastNonWS = buf.length();
					continue;
				}
				advance();

				ec = peek();
				switch(ec ) {
				case '%':
					// <%% is verbatim <%
					advance();
					buf.append("<%");
					lastNonWS = buf.length();
					continue;

				case '#': {
					advance();
					int prev = ec;
					boolean foundEnd = false;
					for(ec = next(); ec != 0; ec = next()) {
						if(ec == '%') {
							ec = peek();
							if(ec == '>' && prev != '%') {
								advance();
								foundEnd = true;
								break;
							}
						}
						prev = ec;
					}
					if(!foundEnd) {
						setPos(start);
						throw parseIssue(LEX_UNBALANCED_EPP_COMMENT);
					}
					continue;
				}

				case '-':
					// trim whitespaces leading up to <%-
					advance();
					buf.setLength(lastNonWS);
					break;

				case '=':
					advance();
					break;
				}
				setPos(start); // Next token will be TOKEN_RENDER_EXPR
				setToken(TOKEN_RENDER_STRING, buf.toString());
				if(buf.length() == 0)
					nextToken();
				return;

			case ' ':
			case '\t':
				buf.append(ec);
				break;

			case '%':
				// %%> is verbatim %>
				buf.append('%');
				ec = peek();
				if(ec == '%' ) {
					advance();
					ec = peek();
					if(ec == '>') {
						advance();
						buf.append('>');
					} else
						buf.append('%');
				}
				lastNonWS = buf.length();
				break;

			default:
				buf.append(ec);
				lastNonWS = buf.length();
			}
		}

		if(buf.length() == 0)
			setToken(TOKEN_END);
		else
			setToken(TOKEN_RENDER_STRING, buf.toString());
	}

	// Called after a '$' has been encountered on input.
	//   - Extracts the preceding string from the buf and resets buf.
	//   - Unless the string is empty, adds a NameExpression that represents the string to the segments slice
	//   - Asks the context to perform interpolation and adds the resulting expression to the segments slice
	//   - Sets the tokenStartPos to the position just after the end of the interpolation expression
	//
	private void handleInterpolation(int start, List<Expression> segments, StringBuilder buf) {
		if(buf.length() > 0) {
			segments.add(new LiteralString(buf.toString(), locator, tokenStartPos, pos() - tokenStartPos));
			buf.setLength(0);
		}
		segments.add(interpolate(start));
		tokenStartPos = pos();
	}

	// Performs interpolation starting at the current position (which must point at the starting '$' character)
	// and returns the resulting expression
	private Expression interpolate(int start) {
		char c = peek();
		if(c == '{') {
			advance();

			// Call parser recursively and expect the ending token to be the ending curly brace
			nextToken();
			Expression expr = parse(TOKEN_RC, true);

			// If the result is a single QualifiedName, then it's actually a variable since the `${var}` is the
			// same as `$var`
			if(expr instanceof QualifiedName) {
				expr = new VariableExpression(expr, locator, start, pos() - start);
			} else if(expr instanceof AccessExpression) {
				AccessExpression access = (AccessExpression)expr;
				if(access.operand instanceof QualifiedName) {
					QualifiedName identifier = (QualifiedName)access.operand;
					expr = new AccessExpression(
							new VariableExpression(identifier, locator, start, identifier.length() + 1),
							access.keys, locator, start, access.length() + 1);
				}
			}
			return new TextExpression(expr, locator, start, pos() - start);
		}

		// Not delimited by curly braces. Must be a single identifier then
		setToken(TOKEN_VARIABLE);
		if(c == ':' || isLowercaseLetter(c) || isDigit(c))
			nextToken();

		if(currentToken != TOKEN_IDENTIFIER) {
			setPos(start);
			throw parseIssue(LEX_MALFORMED_INTERPOLATION);
		}
		Expression textExpr = new QualifiedName(tokenString(), locator, start+ 1, pos() - (start+ 1));
		return new TextExpression(new VariableExpression(textExpr, locator, start, pos() - start), locator, start, pos() - start);
	}

	private boolean isRegexpAcceptable() {
		switch(currentToken) {
		// Operands that can be followed by TOKEN_DIVIDE
		case TOKEN_RP: case TOKEN_RB: case TOKEN_TYPE_NAME: case TOKEN_IDENTIFIER: case TOKEN_BOOLEAN: case TOKEN_INTEGER: case TOKEN_FLOAT: case TOKEN_STRING:
		case TOKEN_HEREDOC: case TOKEN_CONCATENATED_STRING: case TOKEN_REGEXP: case TOKEN_VARIABLE:
			return false;
		default:
			return true;
		}
	}
}
