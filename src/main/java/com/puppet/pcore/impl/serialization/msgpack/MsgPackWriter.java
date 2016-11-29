package com.puppet.pcore.impl.serialization.msgpack;

import com.puppet.pcore.impl.serialization.AbstractWriter;
import com.puppet.pcore.impl.serialization.ExtensionAwarePacker;
import com.puppet.pcore.impl.serialization.IOConsumer;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.util.Map;

public class MsgPackWriter extends AbstractWriter {
	private final MsgPacker extensionPacker;

	MsgPackWriter(Map<String,Object> options, ExtensionAwarePacker packer) {
		super(options, packer);
		extensionPacker = new MsgPacker(MessagePack.newDefaultBufferPacker());
	}

	@Override
	public boolean supportsBinary() {
		return true;
	}

	@Override
	protected byte[] buildPayload(IOConsumer<ExtensionAwarePacker> consumer) throws IOException {
		MessageBufferPacker mp = (MessageBufferPacker)(extensionPacker).packer;
		mp.clear();
		consumer.accept(extensionPacker);
		extensionPacker.flush();
		return mp.toByteArray();
	}
}
