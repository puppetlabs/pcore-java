package com.puppet.pcore.impl.serialization.msgpack;

import com.puppet.pcore.impl.Polymorphic;
import com.puppet.pcore.impl.serialization.ExtensionAwarePacker;
import com.puppet.pcore.impl.serialization.PayloadWriterFunction;
import com.puppet.pcore.serialization.SerializationException;
import com.puppet.pcore.impl.serialization.extension.Extension;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class MsgPacker extends Polymorphic<Void> implements ExtensionAwarePacker {
	private static final DispatchMap dispatchMap = initPolymorphicDispatch(MsgPacker.class, "_write");
	final MessagePacker packer;
	private final Map<Class<?>,Extension<?>> extensionMap = new HashMap<>();

	public MsgPacker(MessagePacker packer) {
		this.packer = packer;
	}

	@Override
	public void flush() throws IOException {
		packer.flush();
	}

	@Override
	public <T> void registerType(byte extensionNumber, Class<T> extClass, PayloadWriterFunction<T> payloadWriterFunction) {
		extensionMap.put(extClass, new Extension<>(extensionNumber, extClass, payloadWriterFunction));
	}

	@Override
	public void write(String val) throws IOException {
		// Bypass dispatch
		_write(val);
	}

	@Override
	public void write(long val) throws IOException {
		// Bypass dispatch
		_write(val);
	}

	@Override
	public void write(int val) throws IOException {
		// Bypass dispatch
		_write(val);
	}

	@Override
	public void write(Object val) throws IOException {
		try {
			dispatch(val);
		} catch(InvocationTargetException e) {
			Throwable te = e.getCause();
			if(te instanceof IOException)
				throw (IOException)te;
			if(!(te instanceof RuntimeException))
				te = new RuntimeException(te);
			throw (RuntimeException)te;
		}
	}

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	void _write(Object val) throws IOException {
		@SuppressWarnings("unchecked") Extension<Object> ed = (Extension<Object>)extensionMap.get(val.getClass());
		if(ed == null)
			throw new SerializationException(String.format("Unable to serialize a %s", val.getClass().getName()));
		@SuppressWarnings("unchecked") byte[] bytes = ed.payloadWriterFunction.apply(val);
		packer.packExtensionTypeHeader(ed.number, bytes.length);
		packer.addPayload(bytes);
	}

	void _write(String val) throws IOException {
		packer.packString(val);
	}

	void _write(Long val) throws IOException {
		packer.packLong(val);
	}

	void _write(Integer val) throws IOException {
		packer.packInt(val);
	}

	void _write(Short val) throws IOException {
		packer.packShort(val);
	}

	void _write(Byte val) throws IOException {
		packer.packByte(val);
	}

	void _write(Float val) throws IOException {
		packer.packFloat(val);
	}

	void _write(Double val) throws IOException {
		packer.packDouble(val);
	}

	void _write(Boolean val) throws IOException {
		packer.packBoolean(val);
	}

	void _write(Void val) throws IOException {
		packer.packNil();
	}

	void _write(byte[] val) throws IOException {
		packer.packBinaryHeader(val.length);
		packer.addPayload(val);
	}
}
