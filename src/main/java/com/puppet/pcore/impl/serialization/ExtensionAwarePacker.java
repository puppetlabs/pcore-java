package com.puppet.pcore.impl.serialization;

import java.io.IOException;

public interface ExtensionAwarePacker {
	void flush() throws IOException;

	<T> void registerType(byte extensionNumber, Class<T> extClass, PayloadWriterFunction<T> payloadWriterFunction);

	void write(String val) throws IOException;

	void write(long val) throws IOException;

	void write(int val) throws IOException;

	void write(Object val) throws IOException;
}
