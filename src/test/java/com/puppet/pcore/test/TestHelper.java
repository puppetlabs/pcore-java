package com.puppet.pcore.test;

import com.puppet.pcore.pspec.Test;
import com.puppet.pcore.pspec.TestExecutable;
import com.puppet.pcore.pspec.TestGroup;
import com.puppet.pcore.regex.Regexp;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.*;

import static com.puppet.pcore.impl.Helpers.map;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
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
			fail(format("%sexpected '%s' to contain '%s'", buildPrefix(messageSupplier.get()), expected, actual));
	}

	public static void assertMatches(String expected, String actual) {
		assertMatches(expected, actual, () -> null);
	}

	public static void assertMatches(String expected, String actual, String message) {
		assertMatches(expected, actual, () -> message);
	}

	public static void assertMatches(String expected, String actual, Supplier<String> messageSupplier) {
		if(actual == null || !Regexp.compile(expected).matcher(actual).find())
			fail(format("%sexpected '%s' to match pattern /%s/", buildPrefix(messageSupplier.get()), actual, expected));
	}

	public static <V> List<DynamicTest> dynamicListTest(List<V> list, Function<V, String> title, Consumer<V> test) {
		return map(list, (value) -> dynamicTest(title.apply(value), () -> test.accept(value)));
	}

	public static <K, V> List<DynamicTest> dynamicMapTest(Map<K, V> map, BiFunction<K, V, String> title, BiConsumer<K, V> test) {
		return map(map.entrySet(),
				(entry) -> dynamicTest(
						title.apply(entry.getKey(), entry.getValue()),
						() -> test.accept(entry.getKey(), entry.getValue())));
	}

	public static List<DynamicNode> dynamicPSpecTest(List<? extends Test> tests) {
		return map(tests, t ->
			t instanceof TestExecutable
					? dynamicTest(t.name, ((TestExecutable)t).test::execute)
					: dynamicContainer(t.name, dynamicPSpecTest(((TestGroup)t).tests)));
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

	public static String readResource(Class<?> origin, String resourceName) {
		try(InputStream stream = origin.getResourceAsStream(resourceName)) {
			if(stream == null)
				fail(format("Could not find resource '%s'", resourceName));

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int cnt;
			byte[] buf = new byte[0x800];
			while((cnt = stream.read(buf)) > 0)
				out.write(buf, 0, cnt);

			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch(IOException e) {
			fail(e.getMessage());
			return null; // not reached
		}
	}
}
