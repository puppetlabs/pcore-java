package com.puppet.pcore.impl.types;

import com.puppet.pcore.impl.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.targetType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetTypeTest {
	private final ObjectType subject = targetType();

	@Test
	@DisplayName("Target-Type is an Object-Type")
	public void targetTypeTest() {
		assertTrue(subject instanceof ObjectType);
		assertEquals(subject.name(), "Target");
	}

	@Test
	@DisplayName("A Target can be created with a string")
	public void targetInstanceTest() {
		assertNotNull(subject.newInstance("www.example.com"));
	}

	@Test
	@DisplayName("A Target can be created with a string and a hash of Data")
	public void targetInstanceTest2() {
		assertNotNull(subject.newInstance("www.example.com", Helpers.asMap("opt-key", "opt value")));
	}

	@Test
	@DisplayName("A created Target is an instance of Target-Type")
	public void targetInstanceTest3() {
		assertTrue(subject.isInstance(subject.newInstance("www.example.com", Helpers.asMap("opt-key", "opt value"))));
	}
}
