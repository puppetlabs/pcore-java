package com.puppet.pcore.impl;

import com.puppet.pcore.impl.types.IntegerType;
import com.puppet.pcore.impl.types.StringType;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.integerType;
import static com.puppet.pcore.impl.types.TypeFactory.stringType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeFactoryTest {
	@Test
	public void intType2() {
		IntegerType i = integerType(-1, 1);
		assertEquals(i.min, -1);
		assertEquals(i.max, 1);
	}

	@Test
	public void stringType2() {
		StringType f = stringType("value");
		assertEquals(f.value, "value");
	}
}
