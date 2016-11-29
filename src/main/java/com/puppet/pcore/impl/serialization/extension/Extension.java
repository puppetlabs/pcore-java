package com.puppet.pcore.impl.serialization.extension;

import com.puppet.pcore.impl.serialization.PayloadWriterFunction;

public class Extension<T> {
	public final Class<T> extClass;
	public final byte number;
	public final PayloadWriterFunction<T> payloadWriterFunction;

	public Extension(byte number, Class<T> extClass, PayloadWriterFunction<T> payloadWriterFunction) {
		this.number = number;
		this.extClass = extClass;
		this.payloadWriterFunction = payloadWriterFunction;
	}
}
