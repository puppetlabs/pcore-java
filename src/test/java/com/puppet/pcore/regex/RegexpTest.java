package com.puppet.pcore.regex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ruby specific regexps that doesn't work using java.util.regex
 *
 * @see <a href=https://www.regular-expressions.info/refrecurse.html">RegexBuddy</a>
 */
public class RegexpTest {
	@Nested
	@DisplayName("Special and Non-Printable Characters")
	public class SpecialAndNonPrintables {
		@Test
		@DisplayName("\\cm\\cj matches a Windows CRLF line break")
		public void testWindowsLineBreak() {
			Regexp rx = Regexp.compile("\\cm\\cj");
			assertTrue(rx.matcher("\r\n").matches());
		}

		@Test
		@DisplayName("{ matches { unless part of quantifier such as {n}")
		public void testNonQuantifierBrace1() {
			Regexp rx = Regexp.compile("a{b}c");
			assertTrue(rx.matcher("a{b}c").matches());
		}

		@Test
		@DisplayName("\\77 matches ?")
		public void testOctalEscapeWOLeadingZero1() {
			Regexp rx = Regexp.compile("\\77");
			assertTrue(rx.matcher("?").matches());
		}

		@Test
		@DisplayName("\\100 matches @")
		public void testOctalEscapeWOLeadingZero2() {
			Regexp rx = Regexp.compile("\\100");
			assertTrue(rx.matcher("@").matches());
		}
	}

	@Nested
	@DisplayName("Quantifiers")
	public class Quantifiers {
		@Test
		@DisplayName("a{,4} matches aaaa, aaa, aa, a, or the empty string")
		public void testGreadyQuantifier() {
			Regexp rx = Regexp.compile("a{,4}");
			assertTrue(rx.matcher("aaaa").matches());
			assertTrue(rx.matcher("aaa").matches());
			assertTrue(rx.matcher("aa").matches());
			assertTrue(rx.matcher("a").matches());
			assertTrue(rx.matcher("").matches());
		}

		@Test
		@DisplayName("a{,4}? finds aaaa, aaa, aa, a, or the empty string")
		public void testGreadyQuantifier2() {
			Regexp rx = Regexp.compile("a{,4}?");
			assertTrue(rx.matcher("aaaa").find());
			assertTrue(rx.matcher("aaa").find());
			assertTrue(rx.matcher("aa").find());
			assertTrue(rx.matcher("a").find());
			assertTrue(rx.matcher("").find());
		}
	}

	@Nested
	@DisplayName("Unicode Characters and Properties")
	public class UnicodeSyntax {
		@Test
		@DisplayName("\\X matches à encoded as U+0061 U+0300, à encoded as U+00E0, ©, etc.")
		public void testGrapheme() {
			Regexp rx = Regexp.compile("^h\\Xllå$");
			assertTrue(rx.matcher("hàllå").matches());
			assertTrue(rx.matcher("h\u00E0llå").matches());

			// @TODO assertTrue. Pending fix for https://github.com/jruby/joni/issues/30
			assertFalse(rx.matcher("h\u0061\u0300llå").matches());
		}

		@Test
		@DisplayName("Unicode category \\p{Letter} matches à encoded as U+00E0")
		public void testUnicodeCategory1() {
			Regexp rx = Regexp.compile("\\p{Letter}");
			assertTrue(rx.matcher("à").matches());
		}

		@Test
		@DisplayName("Unicode category \\p{Symbol} matches © encoded as U+00A9")
		public void testUnicodeCategory2() {
			Regexp rx = Regexp.compile("\\p{Symbol}");
			assertTrue(rx.matcher("©").matches());
		}

		@Test
		@DisplayName("Unicode script \\p{Greek} matches © encoded as U+00A9")
		public void testUnicodeScript1() {
			Regexp rx = Regexp.compile("\\p{Greek}");
			assertTrue(rx.matcher("Ω").matches());
		}

		@Test
		@DisplayName("Negated Unicode property \\P{L} matches © encoded as U+00A9")
		public void testNegatedUnicodeProperty1() {
			Regexp rx = Regexp.compile("\\P{L}");
			assertTrue(rx.matcher("©").matches());
			assertFalse(rx.matcher("q").matches());
		}

		@Test
		@DisplayName("Negated Unicode property \\p{^L} matches © encoded as U+00A9")
		public void testNegatedUnicodeProperty2() {
			Regexp rx = Regexp.compile("\\p{^L}");
			assertTrue(rx.matcher("©").matches());
			assertFalse(rx.matcher("q").matches());
		}

		@Test
		@DisplayName("Negated Unicode property \\P{^L} matches q")
		public void testNegatedUnicodeProperty3() {
			Regexp rx = Regexp.compile("\\P{^L}");
			assertTrue(rx.matcher("q").matches());
			assertFalse(rx.matcher("©").matches());
		}
	}

	@Nested
	@DisplayName("Named Groups and Backreferences")
	public class NamedGroupsAndBackrefs {
		@Test
		@DisplayName("Captures the text matched by \"regex\" into the group \"name\"")
		public void testNamedGroup1() {
			Regexp rx = Regexp.compile("(?'x'abc){3}");
			Matcher m = rx.matcher("abcabcabc");
			assertTrue(m.matches());
			String[] g = m.group("x");
			assertNotNull(g);
			assertEquals("abc", g[0]);
		}

		@Test
		@DisplayName("Two named groups can share the same name")
		public void testNamedGroup2() {
			Regexp rx = Regexp.compile("(?<x>a)|(?<x>b)");
			Matcher m = rx.matcher("a");
			assertTrue(m.matches());
			String[] g = m.group("x");
			assertNotNull(g);
			assertEquals("a", g[0]);
			assertNull(g[1]);

			m = rx.matcher("b");
			assertTrue(m.matches());
			g = m.group("x");
			assertNotNull(g);
			assertNull(g[0]);
			assertEquals("b", g[1]);
		}

		@Test
		@DisplayName("Substituted with the text matched by the named group \\k<name>")
		public void testNamedGSubstition1() {
			Regexp rx = Regexp.compile("(?<x>abc|def)=\\k<x>");
			assertTrue(rx.matcher("abc=abc").matches());
			assertTrue(rx.matcher("def=def").matches());
			assertFalse(rx.matcher("abc=def").matches());
			assertFalse(rx.matcher("def=abc").matches());
		}

		@Test
		@DisplayName("Substituted with the text matched by the named group \\k'name'")
		public void testNamedGSubstition2() {
			Regexp rx = Regexp.compile("(?'x'abc|def)=\\k'x'");
			assertTrue(rx.matcher("abc=abc").matches());
			assertTrue(rx.matcher("def=def").matches());
			assertFalse(rx.matcher("abc=def").matches());
			assertFalse(rx.matcher("def=abc").matches());
		}
	}

	@Nested
	@DisplayName("Special Groups")
	public class SpecialGroups {
		@Test
		@DisplayName("Keep text out of the regex match using \\K")
		public void testSpecialGroup1() {
			Regexp rx = Regexp.compile("s\\Kt");
			Matcher m = rx.matcher("street");

			// @TODO assertTrue. Pending fix for https://github.com/jruby/jruby/issues/4871
			assertFalse(m.matches());
			// assertEquals("t", m.group(0));
		}

		@Test
		@DisplayName("(?(<name>)then|else) where name is the name of a capturing group and then and else are any valid regexes")
		public void testNamedConditional1() {
			Regexp rx = Regexp.compile("(?<one>a)?(?(<one>)b|c)");
			Matcher m = rx.matcher("babxcac");
			assertTrue(m.find());
			assertEquals("ab", m.group(0));
			assertTrue(m.find());
			assertEquals("c", m.group(0));
			assertTrue(m.find());
			assertEquals("c", m.group(0));
			assertFalse(m.find());
		}

		@Test
		@DisplayName("(?(1)then|else) where 1 is the number of a capturing group and then and else are any valid regexes")
		public void testConditional1() {
			Regexp rx = Regexp.compile("(a)?(?(1)b|c)");
			Matcher m = rx.matcher("babxcac");
			assertTrue(m.find());
			assertEquals("ab", m.group(0));
			assertTrue(m.find());
			assertEquals("c", m.group(0));
			assertTrue(m.find());
			assertEquals("c", m.group(0));
			assertFalse(m.find());
		}
	}

	@Nested
	@DisplayName("Recursion, Subroutine Calls, and Balancing Groups")
	public class RecursionAndSubroutines {
		@Test
		@DisplayName("Recursion \\g<0>")
		public void testRecursion1() {
			Regexp rx = Regexp.compile("a\\g<0>?z");
			assertTrue(rx.matcher("az").matches());
			assertTrue(rx.matcher("aazz").matches());
			assertTrue(rx.matcher("aaazzz").matches());
		}

		@Test
		@DisplayName("Recursion \\g'0'")
		public void testRecursion2() {
			Regexp rx = Regexp.compile("a\\g'0'?z");
			assertTrue(rx.matcher("az").matches());
			assertTrue(rx.matcher("aazz").matches());
			assertTrue(rx.matcher("aaazzz").matches());
		}

		@Test
		@DisplayName("Subroutine call \\g<1>")
		public void testSubroutineCall1() {
			Regexp rx = Regexp.compile("a(b\\g<1>?y)z");
			assertTrue(rx.matcher("abyz").matches());
			assertTrue(rx.matcher("abbyyz").matches());
			assertTrue(rx.matcher("abbbyyyz").matches());
		}

		@Test
		@DisplayName("Subroutine call \\g'1'")
		public void testSubroutineCall2() {
			Regexp rx = Regexp.compile("a(b\\g'1'?y)z");
			assertTrue(rx.matcher("abyz").matches());
			assertTrue(rx.matcher("abbyyz").matches());
			assertTrue(rx.matcher("abbbyyyz").matches());
		}

		@Test
		@DisplayName("Relative subroutine call \\g<-1>")
		public void testRelativeSubroutineCall1() {
			Regexp rx = Regexp.compile("a(b\\g<-1>?y)z");
			assertTrue(rx.matcher("abyz").matches());
			assertTrue(rx.matcher("abbyyz").matches());
			assertTrue(rx.matcher("abbbyyyz").matches());
		}

		@Test
		@DisplayName("Relative subroutine call \\g'-1'")
		public void testRelativeSubroutineCall2() {
			Regexp rx = Regexp.compile("a(b\\g'-1'?y)z");
			assertTrue(rx.matcher("abyz").matches());
			assertTrue(rx.matcher("abbyyz").matches());
			assertTrue(rx.matcher("abbbyyyz").matches());
		}

		@Test
		@DisplayName("Relative subroutine call \\g<+1>")
		public void testRelativeSubroutineCall3() {
			Regexp rx = Regexp.compile("\\g<+1>x([ab])");
			assertTrue(rx.matcher("axa").matches());
			assertTrue(rx.matcher("axb").matches());
			assertTrue(rx.matcher("bxa").matches());
			assertTrue(rx.matcher("bxb").matches());
		}

		@Test
		@DisplayName("Relative subroutine call \\g'+1'")
		public void testRelativeSubroutineCall4() {
			Regexp rx = Regexp.compile("\\g'+1'x([ab])");
			assertTrue(rx.matcher("axa").matches());
			assertTrue(rx.matcher("axb").matches());
			assertTrue(rx.matcher("bxa").matches());
			assertTrue(rx.matcher("bxb").matches());
		}

		@Test
		@DisplayName("Named subroutine call \\g<name>")
		public void testNamedSubroutineCall1() {
			Regexp rx = Regexp.compile("a(?<x>b\\g<x>?y)z");
			assertTrue(rx.matcher("abyz").matches());
			assertTrue(rx.matcher("abbyyz").matches());
			assertTrue(rx.matcher("abbbyyyz").matches());
		}

		@Test
		@DisplayName("Named subroutine call \\g'name'")
		public void testNamedSubroutineCall2() {
			Regexp rx = Regexp.compile("a(?'x'b\\g'x'?y)z");
			assertTrue(rx.matcher("abyz").matches());
			assertTrue(rx.matcher("abbyyz").matches());
			assertTrue(rx.matcher("abbbyyyz").matches());
		}

		@Test
		@DisplayName("Subroutine calls capture")
		public void testSubroutineCallsCapture() {
			Regexp rx = Regexp.compile("([ab])\\g'1'");
			Matcher m = rx.matcher("ab");
			assertTrue(m.matches());
			assertEquals("b", m.group(1));
		}
	}
}
