package com.puppet.pcore.test;


import com.puppet.pcore.pspec.SpecEvaluator;
import org.junit.jupiter.api.Assertions;

public class PSpecAssertions implements SpecEvaluator.Assertions {
	public static final PSpecAssertions SINGLETON = new PSpecAssertions();

	@Override
	public void assertEquals(Object a, Object b) {
		Assertions.assertEquals(a, b);
	}

	@Override
	public void fail(String message) {
		Assertions.fail(message);
	}
}

