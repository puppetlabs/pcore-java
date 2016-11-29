package com.puppet.pcore;

public class Symbol implements java.io.Serializable, Comparable<Symbol>, CharSequence {
	private final String content;

	public Symbol(String content) {
		this.content = content;
	}

	@Override
	public char charAt(int index) {
		return content.charAt(index);
	}

	@Override
	public int compareTo(Symbol o) {
		return content.compareTo(o.content);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Symbol && content.equals(((Symbol)o).content);
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	public int length() {
		return content.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return content.subSequence(start, end);
	}

	public String toString() {
		return content;
	}
}
