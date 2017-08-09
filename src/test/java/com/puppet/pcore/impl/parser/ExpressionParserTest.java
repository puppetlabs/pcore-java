package com.puppet.pcore.impl.parser;

import com.puppet.pcore.Default;
import com.puppet.pcore.IssueException;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.puppet.pcore.TestHelper.multiline;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("SameParameterValue")
@DisplayName("The Parser")
public class ExpressionParserTest {
	@Nested
	@DisplayName("can parse primitive")
	class Primitives {
		@Test
		@DisplayName("boolean false")
		public void booleanFalse() {
			assertEquals(constant(false), parse("false"));
		}

		@Test
		@DisplayName("boolean true")
		public void booleanTrue() {
			assertEquals(constant(true), parse("true"));
		}

		@Test
		public void doubleQuotedStringWithControlChars() {
			assertEquals(constant("string\nwith\t\\t, \\s, \\r, and \\n\r\n"), parse
					("\"string\\nwith\\t\\\\t,\\s\\\\s, \\\\r, and \\\\n\\r\\n\""));
		}

		@Test
		public void doubleQuotedStringWithDoubleQuotes() {
			assertEquals(constant("string\"with\"quotes"), parse("\"string\\\"with\\\"quotes\""));
		}

		@Test
		public void doubleQuotedStringWithSingleQoutes() {
			assertEquals(constant("string'with'quotes"), parse("\"string'with'quotes\""));
		}

		@Test
		public void doubleQuotedStringWithSupplimentaryUnicodeChars() {
			assertEquals(constant("x" + new String(Character.toChars(0x1f452)) + "y"), parse
					("\"x\\u{1f452}y\""));
		}

		@Test
		public void doubleQuotedStringWithUnicodeChars() {
			assertEquals(constant("x\u2713y"), parse("\"x\\u2713y\""));
		}

		@Test
		@DisplayName("number with exponent")
		public void floatWithExponent() {
			assertEquals(constant(32e7), parse("32e7"));
		}

		@Test
		@DisplayName("number with negative exponent")
		public void floatWithNegativeExponent() {
			assertEquals(constant(32e-7), parse("32e-7"));
		}

		@Test
		@DisplayName("negative number with fragment and exponent")
		public void negativeFloatWithExponentFragment() {
			assertEquals(constant(-32.3e7), parse("-32.3e7"));
		}

		@Test
		@DisplayName("number with fragment and negative exponent")
		public void floatWithNegativeExponentFragment() {
			assertEquals(constant(32.3e-7), parse("32.3e-7"));
		}

		@Test
		@DisplayName("negative identifier")
		public void negativeIdentifier() {
			assertEquals(negate(identifier("my::type")), parse("-my::type"));
		}

		@Test
		@DisplayName("negative integer")
		public void negativeInteger() {
			assertEquals(constant(-123), parse("-123"));
		}

		@Test
		@DisplayName("positive integer")
		public void positiveInteger() {
			assertEquals(constant(123), parse("123"));
		}

		@Test
		@DisplayName("qualified identifier")
		public void qualifiedIdentifier() {
			assertEquals(identifier("my::type"), parse("my::type"));
		}

		@Test
		@DisplayName("qualified type name")
		public void qualifiedTypeName() {
			assertEquals(typeName("My::Type"), parse("My::Type"));
		}

		@Test
		public void regexp1() {
			assertEquals(regexp("pattern/with/slash"), parse("/pattern\\/with\\/slash/"));
		}

		@Test
		@DisplayName("simple identifier")
		public void simpleIdentifier() {
			assertEquals(identifier("mytype"), parse("mytype"));
		}

		@Test
		@DisplayName("simple type name")
		public void simpleTypeName() {
			assertEquals(typeName("MyType"), parse("MyType"));
		}

		@Test
		public void singleQuotedStringWithDoubleQuotes() {
			assertEquals(constant("string\"with\"quotes"), parse("'string\"with\"quotes'"));
		}

		@Test
		public void singleQuotedStringWithSingleQuotes() {
			assertEquals(constant("string'with'quotes"), parse("'string\\'with\\'quotes'"));
		}

		@Test
		public void singleQuotedStringWithSingleUnrecognizedEscapes() {
			assertEquals(constant("string'with'\\unregognized\\escapes"), parse
					("'string\\'with\\'\\unregognized\\escapes'"));
		}

		@Test
		@DisplayName("undef")
		public void undef() {
			assertEquals(constant(null), parse("undef"));
		}

		@Test
		@DisplayName("zero")
		public void zero() {
			assertEquals(constant(0), parse("0"));
		}

		@Test
		@DisplayName("zero with zero fragment")
		public void zeroFloat() {
			assertEquals(constant(0.0), parse("0.0"));
		}

		@Test
		@DisplayName("zero with fragment and exponent")
		public void zeroFloatWithFragmentAndExponent() {
			assertEquals(constant(0.3e7), parse("0.3e7"));
		}

		@Test
		@DisplayName("zero hex")
		public void zeroHex() {
			assertEquals(integer(0x0, 16), parse("0x0"));
		}
	}

	@Nested
	@DisplayName("parses assignment")
	class Assignment {
		@Test
		@DisplayName("type alias")
		void typeAliasT() {
			assertEquals(typeAlias("MyType", access(typeName("Variant"), asList(typeName("Integer"), typeName("String")))),
					parse("type MyType = Variant[Integer,String]"));
		}
	}

	@Nested
	@DisplayName("parses heredoc with")
	class Heredoc {
		@Test
		@DisplayName("multiple lines")
		void plain() {
			assertEquals(
					heredoc(multiline(
							"This is",
							"heredoc text")),
					parse(multiline(
							"@(END)",
							"This is",
							"heredoc text",
							"END")));
		}

		@Test
		@DisplayName("quoted tag")
		void quotedTag() {
			assertEquals(
					heredoc(multiline(
							"This\tis",
							"heredoc text")),
					parse(multiline(
							"@(\"END\"/t)",
							"This\\tis",
							"heredoc text",
							"END")));
		}

		@Test
		@DisplayName("whitespace after end")
		void wsAfterEnd() {
			assertEquals(
					heredoc(multiline(
							"This is",
							"/* heredoc */ text")),
					parse(multiline(
							"@(END)",
							"This is",
							"/* heredoc */ text",
							"END  ")));
		}

		@Test
		@DisplayName("end tag with trailing characters")
		void endTagEmbedded() {
			assertEquals(
					heredoc(multiline(
							"This is not the",
							"END because there's more",
							"")),
					parse(multiline(
							"@(END)",
							"This is not the",
							"END because there's more",
							"",
							"END")));
		}

		@Test
		@DisplayName("no newline after end")
		void noNLAfterEnd() {
			assertEquals(
					heredoc(multiline(
							"This is",
							"heredoc text")),
					parse(multiline(false,
							"@(END)",
							"This is",
							"heredoc text",
							"END")));
		}

		@Test
		@DisplayName("margin")
		void withMargin() {
			assertEquals(
					heredoc(multiline(
							"This is",
							"heredoc text")),
					parse(multiline(
							"@(END)",
							"    This is",
							"    heredoc text",
							"    | END")));
		}

		@Test
		@DisplayName("margin and newline trim")
		void withMarginAndNewlineTrim() {
			assertEquals(
					heredoc(multiline(false,
							"This is",
							"heredoc text")),
					parse(multiline(
							"@(END)",
							"    This is",
							"    heredoc text",
							"    |- END")));
		}

		@Test
		@DisplayName("syntax and escape specification")
		void syntaxAndEscape() {
			assertEquals(
					heredoc("Tex\tt\\n", "syntax"),
					parse(multiline(
							"@(END:syntax/t)",
							"Tex\\tt\\n",
							"|- END")));
		}

		@Test
		@DisplayName("escaped newlines without preceding whitespace")
		void escapedNewlines() {
			assertEquals(
					heredoc("First Line Second Line"),
					parse(multiline(
							"@(END/L)",
							"First Line\\",
							" Second Line",
							"|- END")));
		}

		@Test
		@DisplayName("escaped newlines with proper margin")
		void escapedNewlinesAndProperMargin() {
			assertEquals(
					heredoc(" First Line  Second Line"),
					parse(multiline(
							"@(END/L)",
							" First Line\\",
							"  Second Line",
							"|- END")));
		}

		@Test
		@DisplayName("multiple heredocs on the same line")
		void multipleOnSameLine() {
			assertEquals(hash(asList(entry(heredoc("hello"), heredoc("world")))), parse(multiline(
					"{ @(foo) => @(bar) }",
					"hello",
					"-foo",
					"world",
					"-bar")));
		}
	}

	@Nested
	@DisplayName("fails heredoc parse when")
	class HeredocFails {
		@Test
		@DisplayName("unterminated tag")
		void unterminatedTag() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(END",
					"text",
					"|- END")));
		}

		@Test
		@DisplayName("unterminated quoted tag")
		void unterminatedQuotedTag() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(\"END)",
					"text",
					"|- END")));
		}

		@Test
		@DisplayName("unsupported flag")
		void unsupportedFlag() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(END/x)",
					"text",
					"|- END")));
		}

		@Test
		@DisplayName("bad start")
		void badStart() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@ (END)",
					"text",
					"|- END")));
		}

		@Test
		@DisplayName("empty tag")
		void emptyTag() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@()",
					"text",
					"|-")));
		}

		@Test
		@DisplayName("empty quoted tag")
		void emptyQuotedTag() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(\"\")",
					"text",
					"|-")));
		}

		@Test
		@DisplayName("more than one tag")
		void multipleTags() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(\"ONE\"\"TWO\")",
					"text",
					"|- TWO")));
		}

		@Test
		@DisplayName("more than one syntax")
		void multipleSyntaxes() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(END:json:yaml)",
					"text",
					"|- END")));
		}

		@Test
		@DisplayName("more than one flags spec")
		void multipleFlagSpecs() {
			assertThrows(IssueException.class, () -> parse(multiline(
					"@(END/n/s)",
					"text",
					"|- END")));
		}
	}

	@Nested
	@DisplayName("will error on")
	class SyntaxErrors {
		@Nested
		@DisplayName("unicode escape")
		class UnicodeEscape {

			@Test
			@DisplayName("illegal characters")
			public void illegalChars() {
				assertThrows(IssueException.class, () -> parse("\"\\u{3g3f}\""));
			}

			@Test
			@DisplayName("illegal start")
			public void illegalStart() {
				assertThrows(IssueException.class, () -> parse("\"\\ug123\""));
			}

			@Test
			@DisplayName("less than 4 digits")
			public void lessThanFourDigits() {
				assertThrows(IssueException.class, () -> parse("\"\\u123\""));
				assertThrows(IssueException.class, () -> parse("\"some bad\\u123 right there\""));
			}

			@Test
			@DisplayName("too long")
			public void tooLong() {
				assertThrows(IssueException.class, () -> parse("\"\\u{134a38f}\""));
				assertThrows(IssueException.class, () -> parse("\"some bad\\u{134a38f} right there\""));
			}

			@Test
			@DisplayName("too short")
			public void tooShort() {
				assertThrows(IssueException.class, () -> parse("\"\\u{1}\""));
				assertThrows(IssueException.class, () -> parse("\"some bad\\u{1} right there\""));
			}

			@Test
			@DisplayName("unterminated")
			public void unterminated() {
				assertThrows(IssueException.class, () -> parse("\"\\u{1f452\""));
				assertThrows(IssueException.class, () -> parse("\"some bad\\u{1f452 right there\""));
			}
		}

		@Test
		@DisplayName("identifier starting with digits")
		public void identifiersStartingWithDigits() {
			assertThrows(IssueException.class, () -> parse("123hey"));
		}

		@Test
		@DisplayName("mixed case of first character in segments of qualified names")
		public void mixedCaseTypeName() {
			assertThrows(IssueException.class, () -> parse("Some::type"));
			assertThrows(IssueException.class, () -> parse("some::Type"));
		}

		@Test
		@DisplayName("single colon")
		public void singleColon() {
			assertThrows(IssueException.class, () -> parse("abc:cde"));
		}

		@Test
		@DisplayName("split arrow")
		public void splitArrow() {
			assertThrows(IssueException.class, () -> parse("{'a' = > 1}"));
		}
	}

	private Parser parser;

	@Test
	public void access1() {
		assertEquals(access(typeName("A"), asList(constant(1), constant("b"))), parse("A[1, 'b']"));
	}

	@Test
	public void array1() {
		assertEquals(array(asList(constant(1), constant(2.3), constant(8))),
				parse("[1, 2.3, 8]"));
	}

	@BeforeEach
	public void createParser() {
		parser = new Parser();
	}

	@Test
	public void emptyArray() {
		assertEquals(array(Collections.emptyList()), parse("[]"));
	}

	@Test
	public void emptyHash() {
		assertEquals(hash(Collections.emptyList()), parse("{}"));
	}

	@Test
	public void hash1() {
		assertEquals(hash(asList(entry(constant("a"), constant(1)), entry(constant("b"), constant(8)))), parse("{'a' => 1, 'b' =>" +
				" 8}"));
	}

	Expression parse(String str) {
		return parser.parse(null, str, false, true);
	}

	private Locator locator = new Locator(null, "dummy source");

	Expression access(Expression expr, List<Expression> parameters) {
		return new AccessExpression(expr, parameters, locator, 0, 0);
	}

	Expression array(List<Expression> expressions) {
		return new ArrayExpression(expressions, locator, 0, 0);
	}

	Expression assignment(Expression a, Expression b) {
		return new AssignmentExpression("=", a, b, locator, 0, 0);
	}

	Expression constant(Object value) {
		if(value == null)
			return new LiteralUndef(locator, 0, 0);
		if(value instanceof String)
			return new LiteralString((String)value, locator, 0, 0);
		if(value instanceof Boolean)
			return new LiteralBoolean((Boolean)value, locator, 0, 0);
		if(value instanceof Double || value instanceof Float)
			return new LiteralFloat(((Number)value).doubleValue(), locator, 0, 0);
		if(value instanceof Number)
			return new LiteralInteger(((Number)value).longValue(), 10, locator, 0, 0);
		if(value instanceof Default)
			return new LiteralDefault(locator, 0, 0);
		fail(format("cannot make constant of a %s", value.getClass().getName()));
		return null;
	}

	KeyedEntry entry(Expression key, Expression value) {
		return new KeyedEntry(key, value, locator, 0, 0);
	}

	Expression heredoc(String value) {
		return new HeredocExpression(new LiteralString(value, locator, 0, 0), null, locator, 0, 0);
	}

	Expression heredoc(String value, String syntax) {
		return new HeredocExpression(new LiteralString(value, locator, 0, 0), syntax, locator, 0, 0);
	}

	Expression hash(List<KeyedEntry> entries) {
		return new HashExpression(entries, locator, 0, 0);
	}

	Expression identifier(String value) {
		return new QualifiedName(value, locator, 0, 0);
	}

	Expression integer(long value, int radix) {
		return new LiteralInteger(value, radix, locator, 0, 0);
	}

	Expression negate(Expression expr) {
		return new UnaryMinusExpression(expr, locator, 0, 0);
	}

	Expression regexp(String value) {
		return new LiteralRegexp(value, locator, 0, 0);
	}

	Expression typeName(String value) {
		return new QualifiedReference(value, locator, 0, 0);
	}

	Expression typeAlias(String name, Expression type) {
		return new TypeAlias(name, type, locator, 0, 0);
	}
}
