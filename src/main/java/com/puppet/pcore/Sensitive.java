package com.puppet.pcore;

import java.util.Objects;

/**
 * Represents some sensitive data. The string representation will redact the contained value.
 */
public class Sensitive {
	private final Object value;

	public Sensitive(Object value) {
		this.value = value;
	}

	public boolean equals(Object o) {
		return o instanceof Sensitive && Objects.equals(value, ((Sensitive)o).value);
	}

	public String toString() {
		return "Sensitive [value redacted]";
	}

	public Object unwrap() {
		return value;
	}
}
