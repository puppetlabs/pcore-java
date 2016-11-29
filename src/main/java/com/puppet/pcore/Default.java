package com.puppet.pcore;

/**
 * The class that represents a default instance (the Symbol :default in Ruby).
 */
public class Default {
	public static final Default SINGLETON = new Default();

	private Default() {
	}
}
