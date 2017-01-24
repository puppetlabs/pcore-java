package com.puppet.pcore;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

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

	private static String buildPrefix(String message) {
		return ((message == null || message.trim().length() == 0) ? "" : message + " ==> ");
	}
}
