package com.puppet.pcore.impl.serialization;

import java.io.IOException;

@FunctionalInterface
public interface PayloadReaderFunction<T> {
	T apply(byte[] data) throws IOException;
}
