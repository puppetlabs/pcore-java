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

	Serializer forOutput(Map<String,Object> options, OutputStream out) throws IOException;
}
