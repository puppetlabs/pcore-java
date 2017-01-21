package com.puppet.pcore.serialization;

import java.io.IOException;
import java.util.List;

/**
 * Protocol specific readers such as MsgPack or JSON capable of reading the primitive scalars:
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
public interface Reader {
	/**
	 * Read an object from the underlying input source
	 *
	 * @return the object that was read
	 */
	Object read() throws IOException;

	/**
	 * Initialize the reader with a new set of values that has been read by some other parser.
	 * Only the JSON reader supports this.
	 *
	 * @param values the already parsed values.
	 */
	void initialize(List<?> values);
}
