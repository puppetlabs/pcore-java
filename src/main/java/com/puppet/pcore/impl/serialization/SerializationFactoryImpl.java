package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.serialization.Reader;
import com.puppet.pcore.serialization.SerializationFactory;
import com.puppet.pcore.serialization.Writer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public abstract class SerializationFactoryImpl implements SerializationFactory {
	private final Pcore pcore;

	public SerializationFactoryImpl(Pcore pcore) {
		this.pcore = pcore;
	}

	@Override
	public DeserializerImpl forInput(InputStream in) throws IOException {
		return new DeserializerImpl(pcore, readerOn(in));
	}

	@Override
	public SerializerImpl forOutput(Map<String,Object> options, OutputStream out) throws IOException {
		return new SerializerImpl(pcore, writerOn(options, out));
	}

	protected abstract Reader readerOn(InputStream in) throws IOException;

	protected abstract Writer writerOn(Map<String,Object> options, OutputStream out) throws IOException;
}
