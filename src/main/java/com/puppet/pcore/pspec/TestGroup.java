package com.puppet.pcore.pspec;

import java.util.List;

public class TestGroup extends Test {
	public List<? extends Test> tests;

	public TestGroup(String name, List<? extends Test> tests) {
		super(name);
		this.tests = tests;
	}
}
