package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.errorType;
import static com.puppet.pcore.test.TestHelper.dynamicListTest;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

public class ErrorTypeTest extends PcoreTestBase {
	private final ObjectType subject = errorType();

	@Test
	@DisplayName("ErrorType is an ObjectType")
	public void errorTypeTest() {
		assertTrue(subject instanceof ObjectType);
		assertEquals(subject.name(), "Error");
	}

	@Test
	@DisplayName("An Error is an instance of ErrorType")
	public void errorInstanceTest() {
		Object error = subject.newInstance("bad things happened");
		assertNotNull(error);
		assertTrue(subject.isInstance(error));
	}

	@Test
	@DisplayName("An Error can be created from a message")
	public void errorInstanceTest2() {
		Object error = subject.newInstance("bad things happened");
		assertNotNull(error);
		assertEquals("bad things happened", subject.getAttribute("message").get(error));
	}

	@Test
	@DisplayName("An Error can be created from a message and a kind")
	public void errorInstanceTest3() {
		Object error = subject.newInstance("bad things happened", "puppet/error");
		assertNotNull(error);
		assertEquals("bad things happened", subject.getAttribute("message").get(error));
		assertEquals("puppet/error", subject.getAttribute("kind").get(error));
	}

	@Test
	@DisplayName("An Error can be created from a message, a kind, and an issue_code")
	public void errorInstanceTest4() {
		Object error = subject.newInstance("bad things happened", "puppet/error", "OOPS");
		assertNotNull(error);
		assertEquals("bad things happened", subject.getAttribute("message").get(error));
		assertEquals("puppet/error", subject.getAttribute("kind").get(error));
		assertEquals("OOPS", subject.getAttribute("issue_code").get(error));
	}

	@Test
	@DisplayName("An Error can be created from a hash with message, kind, and issue_code")
	public void errorInstanceTest5() {
		Object error = subject.newInstance(asMap("message", "bad things happened", "kind", "puppet/error", "issue_code", "OOPS"));
		assertNotNull(error);
		assertEquals("bad things happened", subject.getAttribute("message").get(error));
		assertEquals("puppet/error", subject.getAttribute("kind").get(error));
		assertEquals("OOPS", subject.getAttribute("issue_code").get(error));
	}

	@SuppressWarnings("unchecked")
	@TestFactory
	@DisplayName("ErrorType, when parameterized")
	Iterable<DynamicTest> parsedUsingDefault() {
		return dynamicListTest(
				asList(
						"Error['a']",
						"Error[/a/]",
						"Error[Enum['a', 'b']]",
						"Error[Pattern[/a/, /b/]]",
						"Error['a']",
						"Error[/a/]",
						"Error[Enum['a', 'b']]",
						"Error[Pattern[/a/, /b/]]",
						"Error[default, 'a']",
						"Error[default, /a/]",
						"Error[default, Enum['a', 'b']]",
						"Error[default, Pattern[/a/, /b/]]",
						"Error['a', 'a']",
						"Error[/a/, /a/]",
						"Error[Enum['a', 'b'], Enum['a', 'b']]",
						"Error[Pattern[/a/, /b/], Pattern[/a/, /b/]]"
				),
				(str) -> format("%s presents itself as '%s'", str, str),
				(str) -> assertEquals(str, resolveType(str).toString())
		);
	}
}
