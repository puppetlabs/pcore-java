package com.puppet.pcore.impl.serialization;

import java.io.IOException;

@FunctionalInterface
public interface PayloadWriterFunction<T> {
	byte[] apply(T t) throws IOException;
}
