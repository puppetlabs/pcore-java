package com.puppet.pcore.serialization;

import java.io.IOException;

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
}
