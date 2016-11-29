package com.puppet.pcore.serialization;

import java.io.IOException;

/**
 * Protocol specific writer such as MsgPack or JSON capable of writing the primitive scalars:
 * - Boolean
 * - Integer
 * - Float
 * - String
 * and, by using extensions, also
 * - Array start
 * - Map start
 * - Object start
 * - Sensitive start
 * - Regexp
 * - Version
 * - VersionRange
 * - Timespan
 * - Timestamp
 * - Default
 */
public interface Writer {
	/**
	 * Finish writing (flush output).
	 * @throws IOException
	 */
	void finish() throws IOException;

	/**
	 * Write an object
	 *
	 * @param value
	 * @throws IOException
	 */
	void write(Object value) throws IOException;
}
