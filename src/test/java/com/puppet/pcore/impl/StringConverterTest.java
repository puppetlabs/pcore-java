package com.puppet.pcore.impl;


import com.puppet.pcore.Binary;
import com.puppet.pcore.Default;
import com.puppet.pcore.impl.StringConverter.Format;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.regex.Regexp;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static com.puppet.pcore.test.TestHelper.assertMatches;
import static com.puppet.pcore.test.TestHelper.dynamicMapTest;
import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("The StringConverter")
public class StringConverterTest {
	static final StringConverter converter = StringConverter.singleton;

	@Nested
	@DisplayName("format")
	class FormatTest {
		@Test
		@DisplayName("parses a single character like '%d' as a format")
		public void parsesSingleCharacter() {
			Format fmt = new Format("%d");
			assertEquals('d', fmt.fmt);
			assertFalse(fmt.isAlt);
			assertFalse(fmt.isLeft);
			assertFalse(fmt.isZeroPad);
			assertNull(fmt.width);
			assertNull(fmt.prec);
			assertEquals(fmt.plus, 0);
		}

		@Test
		@DisplayName("alternative form can be given with '%#d'")
		public void parsesAltForm() {
			Format fmt = new Format("%#d");
			assertEquals('d', fmt.fmt);
			assertTrue(fmt.isAlt);
		}

		@Test
		@DisplayName("left adjust can be given with '%-d'")
		public void parsesLeftAdjust() {
			Format fmt = new Format("%-d");
			assertEquals('d', fmt.fmt);
			assertTrue(fmt.isLeft);
		}

		@Test
		@DisplayName("plus sign can be used to indicate how positive numbers are displayed")
		public void parsesPlus() {
			Format fmt = new Format("%+d");
			assertEquals('d', fmt.fmt);
			assertEquals('+', fmt.plus);
		}

		@Test
		@DisplayName("a space can be used to output ' ' instead of '+' for positive numbers")
		public void parsesSpace() {
			Format fmt = new Format("% d");
			assertEquals('d', fmt.fmt);
			assertEquals(' ', fmt.plus);
		}

		@Test
		@DisplayName("padding with zero can be specified with a '0' flag")
		public void parsesZeroPad() {
			Format fmt = new Format("%0d");
			assertEquals('d', fmt.fmt);
			assertTrue(fmt.isZeroPad);
		}

		@Test
		@DisplayName("width can be specified as an integer >= 1")
		public void parsesWidth() {
			Format fmt = new Format("%1d");
			assertEquals('d', fmt.fmt);
			assertNotNull(fmt.width);
			assertEquals(1, fmt.width.intValue());
		}

		@Test
		@DisplayName("precision can be specified as an integer >= 0")
		public void parsesPrec() {
			Format fmt = new Format("%.0d");
			assertEquals('d', fmt.fmt);
			assertNotNull(fmt.prec);
			assertEquals(0, fmt.prec.intValue());
			fmt = new Format("%.10d");
			assertEquals('d', fmt.fmt);
			assertNotNull(fmt.prec);
			assertEquals(10, fmt.prec.intValue());
		}

		@Test
		@DisplayName("width and precision can both be specified")
		public void parsesWidthAndPrec() {
			Format fmt = new Format("%2.3d");
			assertEquals('d', fmt.fmt);
			assertNotNull(fmt.width);
			assertEquals(2, fmt.width.intValue());
			assertNotNull(fmt.prec);
			assertEquals(3, fmt.prec.intValue());
		}

		@ParameterizedTest
		@ValueSource(strings = {"[", "{", "(", "<", "|"})
		@DisplayName("a container delimiter pair can be set with valid delimiter")
		public void parsesDelimiter(String delim) {
			Format fmt = new Format('%' + delim + 'd');
			assertEquals('d', fmt.fmt);
			assertEquals(delim.charAt(0), fmt.leftDelimiter);
		}

		@Test
		@DisplayName("Is an error to specify different delimiters at the same time")
		public void moreThanOneDelmiter() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> new Format("%[{d"));
			assertMatches("Only one of the delimiters", ex.getMessage());
		}

		@Test
		@DisplayName("Is an error to have trailing characters after the format")
		public void trailingCharacters() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> new Format("%dv"));
			assertMatches("The format '%dv' is not a valid format", ex.getMessage());
		}

		@Test
		@DisplayName("Is an error to specify the same flag more than once")
		public void flagMoreThanOnce() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> new Format("%[[d"));
			assertMatches("The same flag can only be used once", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting array")
	class ConvertArrayTest {
		@TestFactory
		@DisplayName("[1, 'hello']")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%s", "[1, 'hello']",
							"%p", "[1, 'hello']",
							"%a", "[1, 'hello']",
							"%<a", "<1, 'hello'>",
							"%[a", "[1, 'hello']",
							"%(a", "(1, 'hello')",
							"%{a", "{1, 'hello'}",
							"% a", "1, 'hello'",

							asMap("format", "%(a", "separator", ""), "(1 'hello')",
							asMap("format", "%|a", "separator", ""), "|1 'hello'|",
							asMap("format", "%(a", "separator", "", "string_formats", asMap(integerType(), "%#x")), "(0x1 'hello')"),
					(fmt, result) -> format("the format %s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(arrayType(), fmt);
						assertEquals(result, converter.convert(asList(1, "hello"), formats));
					});
		}

		@Test
		@DisplayName("multiple rules selects most specific")
		void multipleRulesSpecificPriority() {
			Map<AnyType,Format> formats = asMap(
					arrayType(integerType(), 1, 2), "%(a",
					arrayType(integerType(), 3, 4), "%{a");
			assertEquals("(1, 2)", converter.convert(asList(1, 2), formats));
			assertEquals("{1, 2, 3}", converter.convert(asList(1, 2, 3), formats));
			assertEquals("[1, 2, 3, 4, 5]", converter.convert(asList(1, 2, 3, 4, 5), formats));
		}

		@Test
		@DisplayName("indents elements in alternate mode")
		void indentsInAlternateMode() {
			String result = join(
					"\n",
					"[1, 2, 9, 9,",
					"  [3, 4],",
					"  [5,",
					"    [6, 7]],",
					"  8, 9]"
			);
			Map<AnyType,Format> formats = asMap(arrayType(), asMap("format", "%#a", "separator", ","));
			assertEquals(result, converter.convert(asList(1, 2, 9, 9, asList(3, 4), asList(5, asList(6, 7)), 8, 9), formats));
		}

		@Test
		@DisplayName("treats hashes as nested arrays wrt indentation")
		void hashIndentAsNestedArray() {
			String result = join(
					"\n",
					"[1, 2, 9, 9,",
					"  {3 => 4, 5 => 6},",
					"  [5,",
					"    [6, 7]],",
					"  8, 9]"
			);
			Map<AnyType,Format> formats = asMap(arrayType(), asMap("format", "%#a", "separator", ","));
			assertEquals(result, converter.convert(asList(1, 2, 9, 9, asMap(3, 4, 5, 6), asList(5, asList(6, 7)), 8, 9), formats));
		}

		@Test
		@DisplayName("indents and breaks when a sequence > given width, in alternate mode")
		void altModeIndentsAndBreaks() {
			String result = join(
					"\n",
					"[ 1,",
					"  2,",
					"  90,", // this sequence has length 4 (1,2,90) which is > 3
					"  [3, 4],",
					"  [5,",
					"    [6, 7]],",
					"  8,",
					"  9]"
			);
			Map<AnyType,Format> formats = asMap(arrayType(), asMap("format", "%#3a", "separator", ","));
			assertEquals(result, converter.convert(asList(1, 2, 90, asList(3, 4), asList(5, asList(6, 7)), 8, 9), formats));
		}

		@Test
		@DisplayName("indents and breaks when a sequence (placed last) > given width, in alternate mode")
		void altModeIndentsAndBreaksPlacedLast() {
			String result = join(
					"\n",
					"[ 1,",
					"  2,",
					"  9,", // this sequence has length 3 (1,2,9) which does not cause breaking on each
					"  [3, 4],",
					"  [5,",
					"    [6, 7]],",
					"  8,",
					"  900]" // this sequence has length 4 (8, 900) which causes breaking on each
			);
			Map<AnyType,Format> formats = asMap(arrayType(), asMap("format", "%#3a", "separator", ","));
			assertEquals(result, converter.convert(asList(1, 2, 9, asList(3, 4), asList(5, asList(6, 7)), 8, 900), formats));
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("indents and breaks nested sequences when one is placed first")
		void altModeIndentsAndBreaksPlacedFirst() {
			String result = join(
					"\n",
					"[",
					"  [",
					"    [1, 2,",
					"      [3, 4]]],",
					"  [5,",
					"    [6, 7]],",
					"  8, 900]"
			);
			Map<AnyType,Format> formats = asMap(arrayType(), asMap("format", "%#a", "separator", ","));
			assertEquals(result, converter.convert(asList(asList(asList(1, 2, asList(3, 4))), asList(5, asList(6, 7)), 8, 900), formats));
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(asList(1, 2), asMap(arrayType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Array type - expected one of the characters 'asp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting binary")
	class ConvertBinaryTest {
		final Binary sample = new Binary(new byte[]{'b', 'i', 'n', 'a', 'r', 'y'});

		@Test
		@DisplayName("the binary is converted to strict base64 string unquoted by default (same as %B)")
		void defaultStrictBase64() {
			assertEquals("YmluYXJ5", converter.convert(sample));
		}

		@Test
		@DisplayName("the binary is converted using %p by default when contained in an array")
		void uses_b_whenInArray() {
			assertEquals("[Binary('YmluYXJ5')]", converter.convert(singletonList(sample)));
		}

		@Test
		@DisplayName("%u formats in base64 urlsafe mode")
		void urlSafeWith_u() {
			assertEquals("--__", converter.convert(Binary.fromBase64("++//"), "%u"));
		}

		@Test
		@DisplayName("refuses to do %s on strings containing illegal unicode constructs")
		void quotedWithNonPrintables() {
			Throwable ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(Binary.fromBase64("apa="), "%s"));
			assertMatches("binary data is not valid UTF-8", ex.getMessage());
		}

		@Test
		@DisplayName("%s formats as unquoted string with valid UTF-8 chars")
		void unquotedValidUTF() {
			assertEquals("\uE318", converter.convert(new Binary(new byte[]{(byte)0xee, (byte)0x8c, (byte)0x98}), "%s"));
		}

		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%s", "binary",
							"%#s", "'binary'",
							"%8s", "  binary",
							"%.2s", "bi",
							"%-8s", "binary  ",
							"%p", "Binary('YmluYXJ5')",
							"%b", "YmluYXJ5\n",
							"%11b", "  YmluYXJ5\n",
							"%-11b", "YmluYXJ5\n  ",
							"%.2b", "Ym",
							"%B", "YmluYXJ5",
							"%11B", "   YmluYXJ5",
							"%-11B", "YmluYXJ5   ",
							"%.2B", "Ym",
							"%u", "YmluYXJ5",
							"%11u", "   YmluYXJ5",
							"%-11u", "YmluYXJ5   ",
							"%.2u", "Ym",
							"%t", "Binary",
							"%#t", "'Binary'",
							"%8t", "  Binary",
							"%-8t", "Binary  ",
							"%.3t", "Bin",
							"%T", "BINARY",
							"%#T", "'BINARY'",
							"%8T", "  BINARY",
							"%-8T", "BINARY  ",
							"%.3T", "BIN"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(binaryType(), fmt);
						assertEquals(result, converter.convert(sample, formats));
					});
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(sample, asMap(binaryType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Binary type - expected one of the characters 'bButTsp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting boolean")
	class ConvertBooleanTest {
		@TestFactory
		@DisplayName("true, the format")
		Iterable<DynamicTest> fmtAndResultForTrue() {
			return dynamicMapTest(
					asMap(
							"%t", "true",
							"%#t", "t",
							"%T", "True",
							"%#T", "T",
							"%s", "true",
							"%#s", "'true'",
							"%p", "true",
							"%#p", "'true'",
							"%d", "1",
							"%x", "1",
							"%#x", "0x1",
							"%o", "1",
							"%#o", "01",
							"%b", "1",
							"%#b", "0b1",
							"%#B", "0B1",
							"%e", "1.000000e+00",
							"%f", "1.000000",
							"%.5g", "1.0000",
							"%a", "0x1.0p0",
							"%A", "0X1.0P0",
							"%.1f", "1.0",
							"%y", "yes",
							"%Y", "Yes",
							"%#y", "y",
							"%#Y", "Y"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(booleanType(), fmt);
						assertEquals(result, converter.convert(true, formats));
					});
		}

		@TestFactory
		@DisplayName("false, the format")
		Iterable<DynamicTest> fmtAndResultForFalse() {
			return dynamicMapTest(
					asMap(
							"%t", "false",
							"%#t", "f",
							"%T", "False",
							"%#T", "F",
							"%s", "false",
							"%p", "false",
							"%d", "0",
							"%x", "0",
							"%#x", "0",
							"%o", "0",
							"%#o", "0",
							"%b", "0",
							"%#b", "0",
							"%#B", "0",
							"%e", "0.000000e+00",
							"%E", "0.000000E+00",
							"%f", "0.000000",
							"%.5g", "0.0000",
							"%a", "0x0.0p0",
							"%A", "0X0.0P0",
							"%.1f", "0.0",
							"%y", "no",
							"%Y", "No",
							"%#y", "n",
							"%#Y", "N"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(booleanType(), fmt);
						assertEquals(result, converter.convert(false, formats));
					});
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(true, asMap(booleanType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Boolean type - expected one of the characters 'tTyYdxXobBeEfgGaAsp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting default")
	class ConvertDefaultTest {
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%d", "default",
							"%D", "Default",
							"%#d", "'default'",
							"%#D", "'Default'",
							"%s", "default",
							"%p", "default"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(defaultType(), fmt);
						assertEquals(result, converter.convert(Default.SINGLETON, formats));
					});
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(
					StringConverter.StringFormatException.class,
					() -> converter.convert(Default.SINGLETON, asMap(defaultType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Default type - expected one of the characters 'dDsp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting float")
	class ConvertFloatTest {
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%s", "18.0",
							"%#s", "'18.0'",
							"%5s", " 18.0",
							"%#8s", "  '18.0'",
							"%05.1s", "    1",
							"%p", "18.0000",
							"%7.2p", "     18",

							"%e", "1.800000e+01",
							"%+e", "+1.800000e+01",
							"% e", " 1.800000e+01",
							"%.2e", "1.80e+01",
							"%10.2e", "  1.80e+01",
							"%-10.2e", "1.80e+01  ",
							"%010.2e", "001.80e+01",

							"%E", "1.800000E+01",
							"%+E", "+1.800000E+01",
							"% E", " 1.800000E+01",
							"%.2E", "1.80E+01",
							"%10.2E", "  1.80E+01",
							"%-10.2E", "1.80E+01  ",
							"%010.2E", "001.80E+01",

							"%f", "18.000000",
							"%+f", "+18.000000",
							"% f", " 18.000000",
							"%.2f", "18.00",
							"%10.2f", "     18.00",
							"%-10.2f", "18.00     ",
							"%010.2f", "0000018.00",

							"%g", "18.0000",
							"%10g", "   18.0000",
							"%010g", "00018.0000",
							"%-10g", "18.0000   ",
							"%10.4g", "     18.00",  // precision has no effect

							"%a", "0x1.2p4",
							"%.4a", "0x1.2000p4",
							"%10a", "   0x1.2p4",
							"%010a", "0x0001.2p4",
							"%-10a", "0x1.2p4   ",
							"%A", "0X1.2P4",
							"%.4A", "0X1.2000P4",
							"%10A", "   0X1.2P4",
							"%-10A", "0X1.2P4   ",
							"%010A", "0X0001.2P4",

							// integer formats fully tested for integer
							"%d", "18",
							"%x", "12",
							"%X", "12",
							"%o", "22",
							"%b", "10010",
							"%#B", "0B10010"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(floatType(), fmt);
						assertEquals(result, converter.convert(18.0, formats));
					});
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(18.0, asMap(floatType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Float type - expected one of the characters 'dxXobBeEfgGaAsp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting hash")
	class ConvertHashTest {
		@Test
		@DisplayName("the default string representation is using {} delimiters, joins with '=>' and uses %p for keys and values")
		void braceDelimRocketAndKeyValueP() {
			assertEquals("{'hello' => 'world'}", converter.convert(asMap("hello", "world")));
		}

		@TestFactory
		@DisplayName("{1 => 'world'}")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%s", "{1 => 'world'}",
							"%p", "{1 => 'world'}",
							"%h", "{1 => 'world'}",
							"%a", "[[1, 'world']]",
							"%<h", "<1 => 'world'>",
							"%[h", "[1 => 'world']",
							"%(h", "(1 => 'world')",
							"%{h", "{1 => 'world'}",
							"% h", "1 => 'world'",

							asMap("format", "%(h", "separator2", " "), "(1 'world')",
							asMap("format", "%(h", "separator2", " ", "string_formats", asMap(integerType(), "%#x")), "(0x1 'world')"),
					(fmt, result) -> format("the format %s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(hashType(), fmt);
						assertEquals(result, converter.convert(asMap(1, "world"), formats));
					});
		}

		@TestFactory
		@DisplayName("{1 => 'hello', 2 => 'world'}")
		Iterable<DynamicTest> fmtAndResult2() {
			return dynamicMapTest(
					asMap(
							"%s",
							"{1 => 'hello', 2 => 'world'}",
							asMap("format", "%(h", "separator2", " "),
							"(1 'hello', 2 'world')",
							asMap("format", "%(h", "separator", " >>", "separator2", " <=> ", "string_formats", asMap(integerType(), "%#x")),
							"(0x1 <=> 'hello' >> 0x2 <=> 'world')"),
					(fmt, result) -> format("the format %s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(hashType(), fmt);
						assertEquals(result, converter.convert(asMap(1, "hello", 2, "world"), formats));
					});
		}

		@Test
		@DisplayName("indents elements in alternative mode #")
		void altModeIndentsElements() {
			String result = join(
					"\n",
					"{",
					"  1 => 'hello',",
					"  2 => {",
					"    3 => 'world'",
					"  }",
					"}"
			);
			Map<AnyType,Format> formats = asMap(hashType(), asMap("format", "%#h", "separator", ","));
			assertEquals(result, converter.convert(asMap(1, "hello", 2, asMap(3, "world")), formats));
		}

		@Nested
		@DisplayName("containing an array")
		class ContainingArray {
			@Test
			@DisplayName("the hash and array renders without breaks and indentation by default")
			void noBreakOrIndentDefault() {
				assertEquals("{1 => [1, 2, 3]}", converter.convert(asMap(1, asList(1, 2, 3))));
			}

			@Test
			@DisplayName("the array renders with breaks if so specified")
			void containingAnArray() {
				String result = join(
						"\n",
						"{1 => [ 1,",
						"    2,",
						"    3]}"
				);
				Map<AnyType,Format> formats = asMap(arrayType(), asMap("format", "%#1a", "separator", ","));
				assertEquals(result, converter.convert(asMap(1, asList(1, 2, 3)), formats));
			}

			@Test
			@DisplayName("both hash and array renders with breaks and indentation if so specified for both")
			void hashArrayIndentationAndBreaks() {
				String result = join(
						"\n",
						"{",
						"  1 => [ 1,",
						"    2,",
						"    3]",
						"}"
				);
				Map<AnyType,Format> formats = asMap(
						arrayType(), asMap("format", "%#1a", "separator", ","),
						hashType(), asMap("format", "%#h", "separator", ","));
				assertEquals(result, converter.convert(asMap(1, asList(1, 2, 3)), formats));
			}

			@Test
			@DisplayName("hash, but not array is rendered with breaks and indentation if so specified only for the hash")
			void onlyHashIndentationAndBreaks() {
				String result = join(
						"\n",
						"{",
						"  1 => [1, 2, 3]",
						"}"
				);
				Map<AnyType,Format> formats = asMap(
						arrayType(), asMap("format", "%a", "separator", ","),
						hashType(), asMap("format", "%#h", "separator", ","));
				assertEquals(result, converter.convert(asMap(1, asList(1, 2, 3)), formats));
			}
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(asMap(1, 2), asMap(hashType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Hash type - expected one of the characters 'hasp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting integer")
	class ConvertIntegerTest {
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%s", "18",
							"%4.1s", "   1",
							"%p", "18",
							"%4.2p", "  18",
							"%4.1p", "   1",
							"%#s", "'18'",
							"%#6.4s", "  '18'",
							"%#p", "18",
							"%#6.4p", "    18",
							"%d", "18",
							"%4.1d", "  18",
							"%4.3d", " 018",
							"%x", "12",
							"%4.3x", " 012",
							"%#x", "0x12",
							"%#6.4x", "0x0012",
							"%X", "12",
							"%4.3X", " 012",
							"%#X", "0X12",
							"%#6.4X", "0X0012",
							"%o", "22",
							"%4.2o", "  22",
							"%#o", "022",
							"%#6.4o", "  0022",
							"%b", "10010",
							"%7.6b", " 010010",
							"%#b", "0b10010",
							"%#9.6b", " 0b010010",
							"%#B", "0B10010",
							"%#9.6B", " 0B010010",
							// Integer to float then a float format - fully tested for float
							"%e", "1.800000e+01",
							"%E", "1.800000E+01",
							"%f", "18.000000",
							"%.5g", "18.000",
							"%a", "0x1.2p4",
							"%A", "0X1.2P4",
							"%.1f", "18.0"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(integerType(), fmt);
						assertEquals(result, converter.convert(18, formats));
					});
		}

		@Test
		@DisplayName("produces a unicode char string by using format %c")
		public void producesUnicodeChar() {
			assertEquals("\uD83D\uDE03", converter.convert(0x1f603, "%c"));
		}

		@Test
		@DisplayName("produces a quoted unicode char string by using format %#c")
		public void producesQuotedUnicodeChar() {
			assertEquals("'\uD83D\uDE03'", converter.convert(0x1f603, "%#c"));
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(18, asMap(integerType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Integer type - expected one of the characters 'dxXobBeEfgGaAspc'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting iterator")
	class ConvertIteratorTest {
		@Test
		@DisplayName("the iterator is transformed to an array and formatted using array rules")
		public void iteratorAsArray() {
			assertEquals("[1, 2, 3]", converter.convert(asList(1, 2, 3).iterator()));
		}
	}

	@Nested
	@DisplayName("when converting regexp")
	class ConvertRegexpTest {
		final Regexp sample = Regexp.compile(".*");

		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%s", ".*",
							"%6s", "    .*",
							"%.1s", ".",
							"%-6s", ".*    ",
							"%p", "/.*/",
							"%6p", "  /.*/",
							"%-6p", "/.*/  ",
							"%.2p", "/.",
							"%#s", "'.*'",
							"%#p", "/.*/"),
					(fmt, result) -> format("%s produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(regexpType(), fmt);
						assertEquals(result, converter.convert(sample, formats));
					});
		}
		@Nested
		@DisplayName("that contains flags")
		class ContainsFlags {
			@Test
			@DisplayName("the format %s produces '(?m:[a-z]\\s*)' for expression /[a-z]\\s*/m")
			public void multiLine() {
				assertEquals("(?m:[a-z]\\s*)", converter.convert(Regexp.compile("[a-z]\\s*", Regexp.MULTILINE), "%s"));
			}

			@Test
			@DisplayName("the format %p produces '/(?m:[a-z]\\s*)/' for expression /[a-z]\\s*/m")
			public void multiLine_p() {
				assertEquals("/(?m:[a-z]\\s*)/", converter.convert(Regexp.compile("[a-z]\\s*", Regexp.MULTILINE), "%p"));
			}
		}

		@Test
		@DisplayName("the format %p produces '/foo\\/bar\\/bas/' for expression 'foo/bar\\/baz'")
		public void escapedBackslash_p() {
			assertEquals("/foo\\/bar\\/baz/", converter.convert(Regexp.compile("foo/bar\\/baz"), "%p"));
		}

		@Test
		@DisplayName("the format %s produces 'foo/bar\\/bas' for expression 'foo/bar\\/baz'")
		public void noEscapedBackslash_s() {
			assertEquals("foo/bar\\/baz", converter.convert(Regexp.compile("foo/bar\\/baz"), "%s"));
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(sample, asMap(regexpType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Regexp type - expected one of the characters 'ps'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting string")
	class ConvertStringTest {
		@Test
		@DisplayName("the string value of 'hello world' is 'hello world'")
		public void defaultConvertionFromString() {
			assertEquals("hello world", converter.convert("hello world"));
		}

		@Nested
		@DisplayName("the %p format for string produces")
		class StringPTest {
			final Map<AnyType,Object> stringFormats = singletonMap(stringType(), "%p");

			@Test
			@DisplayName("double quoted result for string that contains control characters")
			public void dquoteForControlChars() {
				assertEquals("\"hello\\tworld.\\r\\nSun is brigth today.\"", converter.convert("hello\tworld.\r\nSun is brigth today.", stringFormats));
			}

			@Test
			@DisplayName("singe quoted result for string without \\or control characters")
			public void squoteNoControlChars() {
				assertEquals("'hello world'", converter.convert("hello world", stringFormats));
			}

			@Test
			@DisplayName("quoted 2-byte unicode chars")
			public void twoByteUniqueChars() {
				assertEquals("\"esc \\u{1B}.\"", converter.convert("esc \u001b.", stringFormats));
			}

			@Test
			@DisplayName("escape for $ in double quoted string")
			public void dquoteEscapeDollar() {
				// Use \n in string to force double quotes
				assertEquals("\"escape the \\$ sign\\n\"", converter.convert("escape the $ sign\n", stringFormats));
			}

			@Test
			@DisplayName("no escape for $ in single quoted string")
			public void squoteNoEscapeDollar() {
				assertEquals("'don\\'t escape the $ sign'", converter.convert("don't escape the $ sign", stringFormats));
			}

			@Test
			@DisplayName("escape for double quote but not for single quote in double quoted string")
			public void dquoteEscapeDquoteNotSquote() {
				// Use \n in string to force double quotes
				assertEquals("\"the ' single and \\\" double quote\\n\"", converter.convert("the ' single and \" double quote\n", stringFormats));
			}

			@Test
			@DisplayName("escape for single quote but not for double quote in single quoted string")
			public void squoteEscapeSquoteNotDquote() {
				assertEquals("'the \\' single and \" double quote'", converter.convert("the ' single and \" double quote", stringFormats));
			}

			@Test
			@DisplayName("escape for last \\")
			public void escapeLastEscape() {
				assertEquals("'escape the last \\'", converter.convert("escape the last \\", stringFormats));
			}
		}

		@TestFactory
		@DisplayName("hello::world")
		Iterable<DynamicTest> convertsLowerQName() {
			return dynamicMapTest(
					asMap(
							"%s", "hello::world",
							"%p", "'hello::world'",
							"%c", "Hello::world",
							"%#c", "'Hello::world'",
							"%C", "Hello::World",
							"%#C", "'Hello::World'",
							"%u", "HELLO::WORLD",
							"%#u", "'HELLO::WORLD'"),
					(fmt, result) -> format("the format %s produces %s", fmt, result),
					(fmt, result) -> assertEquals(result, converter.convert("hello::world", fmt)));
		}

		@TestFactory
		@DisplayName("HELLO::WORLD")
		Iterable<DynamicTest> convertsUpperQName() {
			return dynamicMapTest(
					asMap(
							"%c", "Hello::world",
							"%#c", "'Hello::world'",
							"%C", "Hello::World",
							"%#C", "'Hello::World'",
							"%d", "hello::world",
							"%#d", "'hello::world'"),
					(fmt, result) -> format("the format %s produces %s", fmt, result),
					(fmt, result) -> assertEquals(result, converter.convert("HELLO::WORLD", fmt)));
		}

		@TestFactory
		@DisplayName("using %t")
		Iterable<DynamicTest> trimsInput() {
			return dynamicMapTest(
					asMap(
							"   a b  ", "a b",
							"a b  ", "a b",
							"   a b", "a b"),
					(input, result) -> format("the '%s' is trimmed to '%s'", input, result),
					(input, result) -> assertEquals(result, converter.convert(input, "%t")));
		}

		@TestFactory
		@DisplayName("using %#t")
		Iterable<DynamicTest> trimsInputToQuoted() {
			return dynamicMapTest(
					asMap(
							"   a b  ", "'a b'",
							"a b  ", "'a b'",
							"   a b", "'a b'"),
					(input, result) -> format("the '%s' is trimmed to '%s'", input, result),
					(input, result) -> assertEquals(result, converter.convert(input, "%#t")));
		}

		@Test
		@DisplayName("Width pads a string left with spaces to given width")
		public void widhtPadsLeft() {
			assertEquals("  abcd", converter.convert("abcd", "%6s"));
		}

		@Test
		@DisplayName("Width pads a string right with spaces to given width and - flag")
		public void widhtPadsRight() {
			assertEquals("abcd  ", converter.convert("abcd", "%-6s"));
		}

		@Test
		@DisplayName("Precision truncates the string if precision < length")
		public void precisionTruncates() {
			assertEquals("ab    ", converter.convert("abcd", "%-6.2s"));
		}

		@TestFactory
		@DisplayName("width and precision can be combined")
		Iterable<DynamicTest> combinedWidthAndPrec() {
			return dynamicMapTest(
					asMap(
							"%4.2s", "  he",
							"%4.2p", "  'h",
							"%4.2c", "  He",
							"%#4.2c", "  'H",
							"%4.2u", "  HE",
							"%#4.2u", "  'H",
							"%4.2d", "  he",
							"%#4.2d", "  'h"),
					(fmt, result) -> format("so that 'hello::word' formatted with %s gives \"%s\"", fmt, result),
					(fmt, result) -> assertEquals(result, converter.convert("hello::world", fmt)));
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert("wat", asMap(stringType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of String type - expected one of the characters 'cCudspt'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting type")
	class ConvertTypeTest {
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(asMap(
					"%s", "Integer",
					"%p", "Integer",
					"%#s", "'Integer'",
					"%#p", "Integer"),
					(fmt, result) -> format("'%s' produces %s", fmt, result),
					(fmt, result) -> assertEquals(result, converter.convert(integerType(), fmt)));
		}

		@Test
		@DisplayName("errors when format is not recognized")
		void unrecognizedFormat() {
			Throwable ex = assertThrows(StringConverter.StringFormatException.class, () -> converter.convert(integerType(), asMap(typeType(), "%k")));
			assertMatches("Illegal format 'k' specified for value of Type type - expected one of the characters 'sp'", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when converting undef")
	class ConvertUndefTest {
		@TestFactory
		@DisplayName("the format")
		Iterable<DynamicTest> fmtAndResult() {
			return dynamicMapTest(
					asMap(
							"%u", "undef",
							"%#u", "undefined",
							"%s", "",
							"%#s", "''",
							"%p", "undef",
							"%#p", "'undef'",
							"%n", "nil",
							"%#n", "null",
							"%v", "n/a",
							"%V", "N/A",
							"%d", "NaN",
							"%x", "NaN",
							"%o", "NaN",
							"%b", "NaN",
							"%B", "NaN",
							"%e", "NaN",
							"%E", "NaN",
							"%f", "NaN",
							"%g", "NaN",
							"%G", "NaN",
							"%a", "NaN",
							"%A", "NaN",
							"%7u", "  undef",
							"%-7u", "undef  ",
							"%#10u", " undefined",
							"%#-10u", "undefined ",
							"%7.2u", "     un",
							"%4s", "    ",
							"%#4s", "  ''",
							"%7p", "  undef",
							"%7.1p", "      u",
							"%#8p", " 'undef'",
							"%5n", "  nil",
							"%.1n", "n",
							"%-5n", "nil  ",
							"%#5n", " null",
							"%#-5n", "null ",
							"%5v", "  n/a",
							"%5.2v", "   n/",
							"%-5v", "n/a  ",
							"%5V", "  N/A",
							"%5.1V", "    N",
							"%-5V", "N/A  ",
							"%5d", "  NaN",
							"%5.2d", "   Na",
							"%-5d", "NaN  ",
							"%5x", "  NaN",
							"%5.2x", "   Na",
							"%-5x", "NaN  ",
							"%5o", "  NaN",
							"%5.2o", "   Na",
							"%-5o", "NaN  ",
							"%5b", "  NaN",
							"%5.2b", "   Na",
							"%-5b", "NaN  ",
							"%5B", "  NaN",
							"%5.2B", "   Na",
							"%-5B", "NaN  ",
							"%5e", "  NaN",
							"%5.2e", "   Na",
							"%-5e", "NaN  ",
							"%5E", "  NaN",
							"%5.2E", "   Na",
							"%-5E", "NaN  ",
							"%5f", "  NaN",
							"%5.2f", "   Na",
							"%-5f", "NaN  ",
							"%5g", "  NaN",
							"%5.2g", "   Na",
							"%-5g", "NaN  ",
							"%5G", "  NaN",
							"%5.2G", "   Na",
							"%-5G", "NaN  ",
							"%5a", "  NaN",
							"%5.2a", "   Na",
							"%-5a", "NaN  ",
							"%5A", "  NaN",
							"%5.2A", "   Na",
							"%-5A", "NaN  "),
					(fmt, result) -> format("'%s' produces %s", fmt, result),
					(fmt, result) -> {
						Map<AnyType,Format> formats = asMap(undefType(), fmt);
						assertEquals(result, converter.convert(null, formats));
					});
		}
	}

	@TestFactory
	@DisplayName("literals")
	Iterable<DynamicTest> literalsTruncated() {
		return dynamicMapTest(
				Helpers.<List<?>,String>asMap(
						asList(null, "%.1p"), "u",
						asList(null, "%#.2p"), "'u",
						asList(Default.SINGLETON, "%.1p"), "d",
						asList(true, "%.2s"), "tr",
						asList(true, "%.2y"), "ye"),
				(fmt, result) -> format("the format %s produces '%s' for value %s", fmt.get(1), result, fmt.get(0)),
				(fmt, result) -> assertEquals(result, converter.convert(fmt.get(0), fmt.get(1))));
	}
}
