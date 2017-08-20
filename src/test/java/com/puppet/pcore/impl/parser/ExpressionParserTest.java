package com.puppet.pcore.impl.parser;

import com.puppet.pcore.IssueException;
import com.puppet.pcore.pspec.SpecEvaluator;
import com.puppet.pcore.test.PSpecAssertions;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.puppet.pcore.test.TestHelper.dynamicPSpecTest;
import static com.puppet.pcore.test.TestHelper.multiline;
import static com.puppet.pcore.impl.Helpers.doubleQuote;
import static com.puppet.pcore.test.TestHelper.readResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SameParameterValue")
@DisplayName("The Parser")
public class ExpressionParserTest {
	private static SpecEvaluator evaluator;

	@BeforeAll
	static void init() {
		evaluator = new SpecEvaluator(PSpecAssertions.SINGLETON);
	}

	@TestFactory
	@DisplayName("primitives.pspec")
	public List<DynamicTest> testPrimitives() {
		return dynamicPSpecTest(
				evaluator.createTests("primitives.pspec",
						readResource(getClass(), "primitives.pspec")));
	}

	@TestFactory
	@DisplayName("arithmetic.pspec")
	public List<DynamicTest> testArithmetic() {
		return dynamicPSpecTest(
				evaluator.createTests("arithmetic.pspec",
						readResource(getClass(), "arithmetic.pspec")));
	}

	@Nested
	@DisplayName("can parse primitive")
	class Primitives {
		@Test
		@DisplayName("boolean false")
		public void booleanFalse() {
			assertEquals("false", parse("false"));
		}

		@Test
		@DisplayName("boolean true")
		public void booleanTrue() {
			assertEquals("true", parse("true"));
		}

		@Test
		public void doubleQuotedStringWithControlChars() {
			assertEquals("(concat [\"string\\nwith\\t\\\\t, \\\\s, \\\\r, and \\\\n\\r\\n\"])", parse
					("\"string\\nwith\\t\\\\t,\\s\\\\s, \\\\r, and \\\\n\\r\\n\""));
		}

		@Test
		public void doubleQuotedStringWithDoubleQuotes() {
			assertEquals("(concat [\"string\\\"with\\\"quotes\"])", parse("\"string\\\"with\\\"quotes\""));
		}

		@Test
		public void doubleQuotedStringWithSingleQoutes() {
			assertEquals("(concat [\"string'with'quotes\"])", parse("\"string'with'quotes\""));
		}

		@Test
		public void doubleQuotedStringWithSupplimentaryUnicodeChars() {
			assertEquals("(concat [\"x" + new String(Character.toChars(0x1f452)) + "y\"])", parse
					("\"x\\u{1f452}y\""));
		}

		@Test
		public void doubleQuotedStringWithUnicodeChars() {
			assertEquals("(concat [\"x✓y\"])", parse("\"x\\u2713y\""));
		}

		@Test
		@DisplayName("negative identifier")
		public void negativeIdentifier() {
			assertEquals("(- (qn \"my::type\"))", parse("-my::type"));
		}

		@Test
		@DisplayName("negative integer")
		public void negativeInteger() {
			assertEquals("-123", parse("-123"));
		}

		@Test
		@DisplayName("positive integer")
		public void positiveInteger() {
			assertEquals("123", parse("123"));
		}

		@Test
		@DisplayName("qualified identifier")
		public void qualifiedIdentifier() {
			assertEquals("(qn \"my::type\")", parse("my::type"));
		}

		@Test
		@DisplayName("qualified type name")
		public void qualifiedTypeName() {
			assertEquals("(qr \"My::Type\")", parse("My::Type"));
		}

		@Test
		public void regexp1() {
			assertEquals("(regexp \"pattern/with/slash\")", parse("/pattern\\/with\\/slash/"));
		}

		@Test
		@DisplayName("simple identifier")
		public void simpleIdentifier() {
			assertEquals("(qn \"mytype\")", parse("mytype"));
		}

		@Test
		@DisplayName("simple type name")
		public void simpleTypeName() {
			assertEquals("(qr \"MyType\")", parse("MyType"));
		}

		@Test
		public void singleQuotedStringWithDoubleQuotes() {
			assertEquals("\"string\\\"with\\\"quotes\"", parse("'string\"with\"quotes'"));
		}

		@Test
		public void singleQuotedStringWithSingleQuotes() {
			assertEquals("\"string'with'quotes\"", parse("'string\\'with\\'quotes'"));
		}

		@Test
		public void singleQuotedStringWithSingleUnrecognizedEscapes() {
			assertEquals("\"string'with'\\\\unregognized\\\\escapes\"", parse
					("'string\\'with\\'\\unregognized\\escapes'"));
		}

		@Test
		@DisplayName("undef")
		public void undef() {
			assertEquals("null", parse("undef"));
		}

		@Test
		@DisplayName("zero")
		public void zero() {
			assertEquals("0", parse("0"));
		}

		@Test
		@DisplayName("zero with zero fragment")
		public void zeroFloat() {
			assertEquals("0.0", parse("0.0"));
		}

		@Test
		@DisplayName("zero with fragment and exponent")
		public void zeroFloatWithFragmentAndExponent() {
			assertEquals("3000000.0", parse("0.3e7"));
		}

		@Test
		@DisplayName("zero hex")
		public void zeroHex() {
			assertEquals("0", parse("0x0"));
		}
	}

	@Nested
	@DisplayName("parses calls")
	class Calls {
		@Test
		@DisplayName("transforms statement calls")
		void typeAliasT() {
			assertEquals("(block [(invoke {:functor (qn \"notice\") :args [(call {:functor (qn \"hello\") :args []}) \"world\"]})])", parse("notice hello(), 'world'", true));
		}
	}

	@Nested
	@DisplayName("parses assignment")
	class Assignment {
		@Test
		@DisplayName("type alias")
		void typeAliasT() {
			assertEquals("(type-alias \"MyType\" ([] [(qr \"Variant\") (qr \"Integer\") (qr \"String\")]))",
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
					"(heredoc {:text \"Tex\\tt\\\\n\" :syntax \"syntax\"})",
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
			assertEquals("(hash [(=> (heredoc {:text \"hello\"}) (heredoc {:text \"world\"}))])", parse(multiline(
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

	@BeforeEach
	public void createParser() {
		parser = new Parser();
	}

	String parse(String str) {
		return parse(str, false);
	}

	String parse(String str, boolean program) {
		return parser.parse(null, str, false, !program).toPN().toString();
	}

	String heredoc(String str) {
		return String.format("(heredoc {:text %s})", doubleQuote(str, false));
	}

	String heredoc(String str, String syntax) {
		return String.format("(heredoc {:str \"%s\" :syntax \"%s\"})", str, syntax);
	}
}
