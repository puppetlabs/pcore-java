package com.puppet.pcore;

import org.junit.jupiter.api.DynamicTest;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.map;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class TestHelper {
	public static void assertIncludes(String expected, String actual) {
		assertIncludes(expected, actual, () -> null);
	}

	public static void assertIncludes(String expected, String actual, String message) {
		assertIncludes(expected, actual, () -> message);
	}

	public static void assertIncludes(String expected, String actual, Supplier<String> messageSupplier) {
		if(actual == null || !actual.contains(expected))
			fail(String.format("%sexpected '%s' to contain '%s'", buildPrefix(messageSupplier.get()), expected, actual));
	}

	public static void assertMatches(String expected, String actual) {
		assertMatches(expected, actual, () -> null);
	}

	public static void assertMatches(String expected, String actual, String message) {
		assertMatches(expected, actual, () -> message);
	}

	public static void assertMatches(String expected, String actual, Supplier<String> messageSupplier) {
		if(actual == null || !Pattern.compile(expected).matcher(actual).find())
			fail(String.format("%sexpected '%s' to match pattern /%s/", buildPrefix(messageSupplier.get()), actual, expected));
	}

	public static <K, V> Iterable<DynamicTest> dynamicMapTest(Map<K, V> map, BiFunction<K, V, String> title, BiConsumer<K, V> test) {
		return map(map.entrySet(),
				(entry) -> dynamicTest(
						title.apply(entry.getKey(), entry.getValue()),
						() -> test.accept(entry.getKey(), entry.getValue())));
	}

	private static String buildPrefix(String message) {
		return ((message == null || message.trim().length() == 0) ? "" : message + " ==> ");
	}

	public static String multiline(String ...lines) {
		return multiline(true, lines);
	}

	public static String multiline(boolean trailingNL, String ...lines) {
		StringBuilder bld = new StringBuilder();
		int top = lines.length;
		if(top > 0) {
			bld.append(lines[0]);
			for(int idx = 1; idx < lines.length; ++idx) {
				bld.append('\n');
				bld.append(lines[idx]);
			}
		}
		if(trailingNL)
			bld.append('\n');
		return bld.toString();
	}
}
