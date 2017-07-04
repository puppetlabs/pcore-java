package com.puppet.pcore.impl.types;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("Type Calculator")
public class TypeCalculatorTest {
	@Test
	@DisplayName("infers unknown types to Runtime['java', '<class name>'")
	void inferRuntime() {
		assertEquals(runtimeType("java", getClass().getName()), infer(this));
	}
}
