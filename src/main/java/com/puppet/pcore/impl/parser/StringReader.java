package com.puppet.pcore.impl.parser;

class StringReader {
	private String text;
	private int pos;

	void reset(String text) {
		this.text = text;
		pos = 0;
	}

	String from(int start) {
		return text.substring(start, pos);
	}

	char peek() {
		return pos < text.length() ? text.charAt(pos) : 0;
	}

	int pos() {
		return pos;
	}

	char next() {
		return pos < text.length() ? text.charAt(pos++) : 0;
	}

	void advance() {
		++pos;
	}

	void setPos(int pos) {
		this.pos = pos;
	}
}
