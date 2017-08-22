package com.puppet.pcore.pspec;

public abstract class Node {
	public final String description;

	protected Node(String description) {
		this.description = description;
	}

	public abstract Test createTest();
}
