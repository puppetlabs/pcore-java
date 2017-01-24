package com.puppet.pcore.serialization;

import java.io.IOException;

/**
 * An instance capable of serializing objects to some output target
 */
public interface Serializer {
	/**
	 * Flush any remaining bytes to the output target.
	 *
	 * @throws IOException propagated from the underlying writer
	 */
	void finish() throws IOException;

	/**
	 * Write next object.
	 * @param value the value to write
	 * @throws IOException propagated from the underlying writer
	 */
	void write(Object value) throws IOException;
}
