package com.puppet.pcore.impl.serialization.msgpack;

import com.puppet.pcore.impl.serialization.ExtensionAwareUnpacker;
import com.puppet.pcore.impl.serialization.PayloadReaderFunction;
import com.puppet.pcore.impl.serialization.SerializationException;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ExtensionValue;
import org.msgpack.value.Variable;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class MsgUnpacker implements ExtensionAwareUnpacker {

	final MessageUnpacker unpacker;
	private final Map<Byte,PayloadReaderFunction<?>> extensionMap = new HashMap<>();
	private final Variable variable = new Variable();

	public MsgUnpacker(MessageUnpacker unpacker) {
		this.unpacker = unpacker;
	}

	@Override
	public Object read() throws IOException {
		next();
		switch(variable.getValueType()) {
		case ARRAY:
		case MAP:
			// TODO:
			return null;
		case BINARY:
			return variable.asRawValue().asByteArray();
		case BOOLEAN:
			return variable.asBooleanValue().getBoolean();
		case EXTENSION:
			ExtensionValue ev = variable.asExtensionValue();
			PayloadReaderFunction plr = extensionMap.get(ev.getType());
			if(plr == null)
				throw new SerializationException(format("Invalid input. %d is not a valid extension number", ev.getType()));
			return plr.apply(ev.getData());
		case FLOAT:
			return variable.asFloatValue().toDouble();
		case INTEGER:
			return variable.asIntegerValue().toLong();
		case NIL:
			return null;
		case STRING:
			return variable.asStringValue().asString();
		default:
			throw new SerializationException(format("Invalid input. Unknown value type '%s'", variable.getValueType().name()));
		}
	}

	@Override
	public byte[] readBytes() throws IOException {
		next();
		return variable.asRawValue().asByteArray();
	}

	@Override
	public int readInt() throws IOException {
		next();
		return variable.asIntegerValue().asInt();
	}

	@Override
	public long readLong() throws IOException {
		next();
		return variable.asIntegerValue().asLong();
	}

	@Override
	public String readString() throws IOException {
		next();
		return variable.asStringValue().asString();
	}

	@Override
	public void registerType(byte extensionNumber, PayloadReaderFunction<?> payloadReaderFunction) {
		extensionMap.put(extensionNumber, payloadReaderFunction);
	}

	private void next() throws IOException {
		if(!unpacker.hasNext())
			throw new EOFException();
		unpacker.unpackValue(variable);
	}
}

