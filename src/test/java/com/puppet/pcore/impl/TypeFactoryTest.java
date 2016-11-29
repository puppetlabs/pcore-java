package com.puppet.pcore.impl;

import com.puppet.pcore.impl.types.FloatType;
import com.puppet.pcore.impl.types.StringType;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.floatType;
import static com.puppet.pcore.impl.types.TypeFactory.stringType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeFactoryTest {
	@Test
	public void floatType2() {
		FloatType f = floatType(-0.1, 1.2);
		assertEquals(f.min, -0.1);
		assertEquals(f.max, 1.2);
	}

	@Test
	public void stringType2() {
		StringType f = stringType("value");
		assertEquals(f.value, "value");
	}
}
