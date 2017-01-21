package com.puppet.pcore.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * A factory capable of creating a {@link Serializer} and {@link Deserializer} for
 * a known protocol. There is one factory per protocol.
 */
public interface SerializationFactory {
	String JSON = "JSON";

	String MSGPACK = "MessagePack";

	Deserializer forInput(InputStream in) throws IOException;

	/**
	 * Returns a deserializer that can be initialize with input data that has already been
	 * parsed by another parser into a list of values. This highly specialized deserializer
	 * is used when reading a catalog containing rich data chunks. All chunks share the
	 * same tabulation context.
	 *
	 * @return A deserializer that must be initialized with list data before a read can take place
	 */
	Deserializer forInputChunks();

	Serializer forOutput(Map<String,Object> options, OutputStream out) throws IOException;
}
