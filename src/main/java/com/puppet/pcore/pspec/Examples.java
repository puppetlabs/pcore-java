package com.puppet.pcore.pspec;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.map;

public class Examples extends Node {
	public final List<? extends Node> children;

	public Examples(String description, List<? extends Node> children) {
		super(description);
		this.children = children;
	}

	@Override
	public Test createTest() {
		return new TestGroup(description, map(children, Node::createTest));
	}
}
