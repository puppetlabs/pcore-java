package com.puppet.pcore.impl.serialization.msgpack;

import com.puppet.pcore.impl.serialization.AbstractReader;
import com.puppet.pcore.impl.serialization.ExtensionAwareUnpacker;
import com.puppet.pcore.impl.serialization.IOFunction;
import org.msgpack.core.MessagePack;
import org.msgpack.core.buffer.ArrayBufferInput;

import java.io.IOException;

public class MsgPackReader extends AbstractReader {
	private final ArrayBufferInput buffer = new ArrayBufferInput(new byte[0]);
	private final MsgUnpacker extensionUnpacker = new MsgUnpacker(MessagePack.newDefaultUnpacker(buffer));

	protected MsgPackReader(ExtensionAwareUnpacker unpacker) {
		super(unpacker);
	}

	@Override
	protected <T> T readPayload(byte[] data, IOFunction<ExtensionAwareUnpacker,T> block) throws IOException {
		buffer.reset(data);
		return block.apply(extensionUnpacker);
	}
}
