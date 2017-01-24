package com.puppet.pcore.serialization;

import java.io.IOException;

/**
 * An instance capable of deserializing objects from some input source
 */
public interface Deserializer {
	/**
	 * Read the next object from the source
	 * @return the object that was read
	 * @throws IOException propagated from the underlying reader
	 */
	Object read() throws IOException;

	/**
	 * @return the reader used by this deserializer
	 */
	Reader getReader();
}
