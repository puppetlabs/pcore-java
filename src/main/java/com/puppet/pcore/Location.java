package com.puppet.pcore;

public interface Location {
	/**
	 * The name of the source.
	 * @return name of source
	 */
	String sourceName();

	/**
	 * Line in the source. First line is 1.
	 * @return the line
	 */
	int line();

	/**
	 * Position on line. First position is 1.
	 * @return the position in line
	 */
	int pos();

	/**
	 * Append this location to the given message
	 * @param message the message
	 * @return the given message with this location appended
	 */
	String appendLocation(String message);
}
