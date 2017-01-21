package com.puppet.pcore.impl.serialization.msgpack;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.impl.serialization.SerializationFactoryImpl;
import com.puppet.pcore.serialization.Reader;
import com.puppet.pcore.serialization.Writer;
import org.msgpack.core.MessagePack;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class MsgPackSerializationFactory extends SerializationFactoryImpl {
	@Override
	protected Reader reader() {
		throw new UnsupportedOperationException(getClass().getName() + " does not support partial read of data chunks");
	}

	@Override
	public Reader readerOn(InputStream in) {
		return new MsgPackReader(new MsgUnpacker(MessagePack.newDefaultUnpacker(in)));
	}

	@Override
	public Writer writerOn(Map<String,Object> options, OutputStream out) {
		return new MsgPackWriter(options, new MsgPacker(MessagePack.newDefaultPacker(out)));
	}
}
