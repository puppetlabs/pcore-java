package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The DefaultExpressionParser")
public class ExpressionParserTest {
	@Nested
	@DisplayName("can parse primitive")
	class Primitives {
		@Test
		@DisplayName("boolean false")
		public void booleanFalse() {
			assertEquals(constant(false), parser.parse("false"));
		}

		@Test
		@DisplayName("boolean true")
		public void booleanTrue() {
			assertEquals(constant(true), parser.parse("true"));
		}

		@Test
		public void doubleQuotedStringWithControlChars() {
			assertEquals(constant("string\nwith\t\\t, \\s, \\r, and \\n\r\n"), parser.parse
					("\"string\\nwith\\t\\\\t,\\s\\\\s, \\\\r, and \\\\n\\r\\n\""));
		}

		@Test
		public void doubleQuotedStringWithDoubleQuotes() {
			assertEquals(constant("string\"with\"quotes"), parser.parse("\"string\\\"with\\\"quotes\""));
		}

		@Test
		public void doubleQuotedStringWithSingleQoutes() {
			assertEquals(constant("string'with'quotes"), parser.parse("\"string'with'quotes\""));
		}

		@Test
		public void doubleQuotedStringWithSupplimentaryUnicodeChars() {
			assertEquals(constant("x" + new String(Character.toChars(0x1f452)) + "y"), parser.parse
					("\"x\\u{1f452}y\""));
		}

		@Test
		public void doubleQuotedStringWithUnicodeChars() {
			assertEquals(constant("x\u2713y"), parser.parse("\"x\\u2713y\""));
		}

		@Test
		@DisplayName("number with fragment and exponent")
		public void floatWithExponentFragment() {
			assertEquals(constant(32.3e7), parser.parse("32.3e7"));
		}

		@Test
		@DisplayName("negative number with fragment and exponent")
		public void negativeFloatWithExponentFragment() {
			assertEquals(constant(-32.3e7), parser.parse("-32.3e7"));
		}

		@Test
		@DisplayName("negative identifier")
		public void negativeIdentifier() {
			assertEquals(negate(identifier("my::type")), parser.parse("-my::type"));
		}

		@Test
		@DisplayName("negative integer")
		public void negativeInteger() {
			assertEquals(constant(-123), parser.parse("-123"));
		}

		@Test
		@DisplayName("positive integer")
		public void positiveInteger() {
			assertEquals(constant(123), parser.parse("123"));
		}

		@Test
		@DisplayName("qualified identifier")
		public void qualifiedIdentifier() {
			assertEquals(identifier("my::type"), parser.parse("my::type"));
		}

		@Test
		@DisplayName("qualified type name")
		public void qualifiedTypeName() {
			assertEquals(typeName("My::Type"), parser.parse("My::Type"));
		}

		@Test
		public void regexp1() {
			assertEquals(regexp("pattern/with/slash"), parser.parse("/pattern\\/with\\/slash/"));
		}

		@Test
		@DisplayName("simple identifier")
		public void simpleIdentifier() {
			assertEquals(identifier("mytype"), parser.parse("mytype"));
		}

		@Test
		@DisplayName("simple type name")
		public void simpleTypeName() {
			assertEquals(typeName("MyType"), parser.parse("MyType"));
		}

		@Test
		public void singleQuotedStringWithDoubleQuotes() {
			assertEquals(constant("string\"with\"quotes"), parser.parse("'string\"with\"quotes'"));
		}

		@Test
		public void singleQuotedStringWithSingleQuotes() {
			assertEquals(constant("string'with'quotes"), parser.parse("'string\\'with\\'quotes'"));
		}

		@Test
		public void singleQuotedStringWithSingleUnrecognizedEscapes() {
			assertEquals(constant("string'with'\\unregognized\\escapes"), parser.parse
					("'string\\'with\\'\\unregognized\\escapes'"));
		}

		@Test
		@DisplayName("undef")
		public void undef() {
			assertEquals(constant(null), parser.parse("undef"));
		}

		@Test
		@DisplayName("zero")
		public void zero() {
			assertEquals(constant(0), parser.parse("0"));
		}

		@Test
		@DisplayName("zero with zero fragment")
		public void zeroFloat() {
			assertEquals(constant(0.0), parser.parse("0.0"));
		}

		@Test
		@DisplayName("zero with fragment and exponent")
		public void zeroFloatWithFragmentAndExponent() {
			assertEquals(constant(0.3e7), parser.parse("0.3e7"));
		}

		@Test
		@DisplayName("zero hex")
		public void zeroHex() {
			assertEquals(constant(0x0), parser.parse("0x0"));
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
				assertThrows(ParseException.class, () -> parser.parse("\"\\u{3g3f}\""));
			}

			@Test
			@DisplayName("illegal start")
			public void illegalStart() {
				assertThrows(ParseException.class, () -> parser.parse("\"\\ug123\""));
			}

			@Test
			@DisplayName("less than 4 digits")
			public void lessThanFourDigits() {
				assertThrows(ParseException.class, () -> parser.parse("\"\\u123\""));
				assertThrows(ParseException.class, () -> parser.parse("\"some bad\\u123 right there\""));
			}

			@Test
			@DisplayName("too long")
			public void tooLong() {
				assertThrows(ParseException.class, () -> parser.parse("\"\\u{134a38f}\""));
				assertThrows(ParseException.class, () -> parser.parse("\"some bad\\u{134a38f} right there\""));
			}

			@Test
			@DisplayName("too short")
			public void tooShort() {
				assertThrows(ParseException.class, () -> parser.parse("\"\\u{1}\""));
				assertThrows(ParseException.class, () -> parser.parse("\"some bad\\u{1} right there\""));
			}

			@Test
			@DisplayName("unterminated")
			public void unterminated() {
				assertThrows(ParseException.class, () -> parser.parse("\"\\u{1f452\""));
				assertThrows(ParseException.class, () -> parser.parse("\"some bad\\u{1f452 right there\""));
			}
		}

		@Test
		@DisplayName("identifier starting with digits")
		public void identifiersStartingWithDigits() {
			assertThrows(ParseException.class, () -> parser.parse("123hey"));
		}

		@Test
		@DisplayName("mixed case of first character in segments of qualified names")
		public void mixedCaseTypeName() {
			assertThrows(ParseException.class, () -> parser.parse("Some::type"));
			assertThrows(ParseException.class, () -> parser.parse("some::Type"));
		}

		@Test
		@DisplayName("non unary minus")
		public void nonUnaryMinus() {
			assertThrows(ParseException.class, () -> parser.parse("- y"));
		}

		@Test
		@DisplayName("single colon")
		public void singleColon() {
			assertThrows(ParseException.class, () -> parser.parse("abc:cde"));
		}

		@Test
		@DisplayName("split arrow")
		public void splitArrow() {
			assertThrows(ParseException.class, () -> parser.parse("{'a' = > 1}"));
		}
	}
	private static final ExpressionFactory factory = DefaultExpressionFactory.SINGLETON;
	private DefaultExpressionParser parser;

	@Test
	public void access1() {
		assertEquals(access(typeName("A"), asList(constant(1), constant("b"))), parser.parse("A[1, 'b']"));
	}

	@Test
	public void array1() {
		assertEquals(array(asList(constant(1), constant(2.3), constant(8))),
				parser.parse("[1, 2.3, 8]"));
	}

	@BeforeEach
	public void createParser() {
		parser = new DefaultExpressionParser(factory);
	}

	@Test
	public void emptyArray() {
		assertEquals(array(Collections.emptyList()), parser.parse("[]"));
	}

	@Test
	public void emptyHash() {
		assertEquals(hash(Collections.emptyList()), parser.parse("{}"));
	}

	@Test
	public void hash1() {
		assertEquals(hash(asList(constant("a"), constant(1), constant("b"), constant(8))), parser.parse("{'a' => 1, 'b' =>" +
				" 8}"));
	}

	Expression access(Expression expr, List<Expression> parameters) {
		return factory.access(expr, parameters, "", 0, 0);
	}

	Expression array(List<Expression> expressions) {
		return factory.array(expressions, "", 0, 0);
	}

	Expression constant(Object value) {
		return factory.constant(value, "", 0, 0);
	}

	Expression hash(List<Expression> expressions) {
		return factory.hash(expressions, "", 0, 0);
	}

	Expression identifier(String value) {
		return factory.identifier(value, "", 0, 0);
	}

	Expression negate(Expression expr) {
		return factory.negate(expr, "", 0, 0);
	}

	Expression regexp(String value) {
		return factory.regexp(value, "", 0, 0);
	}

	Expression typeName(String value) {
		return factory.typeName(value, "", 0, 0);
	}
}
