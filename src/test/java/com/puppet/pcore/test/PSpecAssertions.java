package com.puppet.pcore.test;


import org.junit.jupiter.api.Assertions;

public class PSpecAssertions implements com.puppet.pcore.pspec.Assertions {
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

