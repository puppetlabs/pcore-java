package com.puppet.pcore.impl.serialization.extension;

public class SensitiveStart implements NotTabulated {
	public static final SensitiveStart SINGLETON = new SensitiveStart();

	private SensitiveStart() {
	}

	public boolean equals(Object o) {
		return this == o;
	}

	public int hashCode() {
		return 3;
	}
}
