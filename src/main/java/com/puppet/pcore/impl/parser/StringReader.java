package com.puppet.pcore.impl.parser;

public class StringReader {
	private String text;
	private int pos;

	/**
	 * Reset this reader with new contents. Position is set to zero so that
	 * the first character of the new text is returned by the first call
	 * to {@link #next()}
	 * @param text the new text
	 */
	void reset(String text) {
		this.text = text;
		pos = 0;
	}

	/**
	 * Returns a string that starts at start position and ends at the
	 * current position.
	 * @param start the start position
	 * @return the string between start and the current position
	 */
	public String from(int start) {
		return text.substring(start, pos);
	}

	/**
	 * Returns the character at the current position without changing the
	 * position.
	 * @return The character at the current position or 0 if at end of text.
	 */
	public char peek() {
		return pos < text.length() ? text.charAt(pos) : 0;
	}

	/**
	 * Scans the text from the current position and until either the
	 * given character is found or the end of text is reached. Advances
	 * the position only if the character is found.
	 * @param c the character to search for
	 * @return true if the character is found
	 */
	public boolean find(char c) {
		int idx = text.indexOf(c, pos);
		if(idx >= pos) {
			pos = idx;
			return true;
		}
		return false;
	}

	/**
	 * @return the current position
	 */
	public int pos() {
		return pos;
	}

	/**
	 * Returns the character at the current position and advances the
	 * position to the next character.
	 * @return The character at the current position or 0 if at end of text.
	 */
	public char next() {
		return pos < text.length() ? text.charAt(pos++) : 0;
	}

	/**
	 * Advances the position to the next character
	 */
	public void advance() {
		++pos;
	}

	/**
	 * Sets the current position.
	 */
	public void setPos(int pos) {
		this.pos = pos;
	}
}
