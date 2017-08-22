package com.puppet.pcore.pspec;

public class TestExecutable extends Test {
	public final Executable test;

	public TestExecutable(String name, Executable test) {
		super(name);
		this.test = test;
	}
}
