package com.puppet.pcore.impl.serialization;

import java.io.IOException;

public interface ExtensionAwareUnpacker {
	Object read() throws IOException;

	byte[] readBytes() throws IOException;

	int readInt() throws IOException;

	long readLong() throws IOException;

	String readString() throws IOException;

	void registerType(byte extensionNumber, PayloadReaderFunction<?> payloadReaderFunction);
}
