package com.puppet.pcore.impl.serialization.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.puppet.pcore.impl.Polymorphic;
import com.puppet.pcore.impl.serialization.ExtensionAwarePacker;
import com.puppet.pcore.impl.serialization.PayloadWriterFunction;
import com.puppet.pcore.serialization.SerializationException;
import com.puppet.pcore.impl.serialization.extension.Extension;
import com.puppet.pcore.impl.serialization.extension.SequenceStart;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class JsonPacker extends Polymorphic<Void> implements ExtensionAwarePacker {

	private static final DispatchMap dispatchMap = initPolymorphicDispatch(JsonPacker.class, "_write");

	private static final JsonFactory factory = new JsonFactory();
	private final Map<Class<?>,Extension<?>> extensionMap = new HashMap<>();
	private final JsonGenerator generator;
	private final Stack<Integer> nested = new Stack<>();
	private final boolean verbose;

	public JsonPacker(OutputStream out, Map<String,Object> options) throws IOException {
		generator = factory.createGenerator(new BufferedOutputStream((out)));
		generator.writeStartArray();
		this.verbose = booleanOpt("verbose", false, options);
		if(verbose)
			generator.useDefaultPrettyPrinter();
	}

	@Override
	public void flush() throws IOException {
		generator.writeEndArray();
		generator.flush();
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

		generator.writeStartArray();
		generator.writeNumber(ed.number);
		nested.push(null);
		ed.payloadWriterFunction.apply(val);
		nested.pop();
		if(val instanceof SequenceStart && ((SequenceStart)val).sequenceSize() > 0)
			nested.push(((SequenceStart)val).sequenceSize());
		else {
			generator.writeEndArray();
			afterElement();
		}
	}

	void _write(String val) throws IOException {
		generator.writeString(val);
		afterElement();
	}

	void _write(Long val) throws IOException {
		generator.writeNumber(val);
		afterElement();
	}

	void _write(Integer val) throws IOException {
		generator.writeNumber(val);
		afterElement();
	}

	void _write(Short val) throws IOException {
		generator.writeNumber(val);
		afterElement();
	}

	void _write(Byte val) throws IOException {
		generator.writeNumber(val);
		afterElement();
	}

	void _write(Float val) throws IOException {
		generator.writeNumber(val);
		afterElement();
	}

	void _write(Double val) throws IOException {
		generator.writeNumber(val);
		afterElement();
	}

	void _write(Boolean val) throws IOException {
		generator.writeBoolean(val);
		afterElement();
	}

	void _write(Void val) throws IOException {
		generator.writeNull();
		afterElement();
	}

	void afterElement() throws IOException {
		Integer remainingCount = nested.empty() ? null : nested.lastElement();
		if(remainingCount == null)
			return;

		if(remainingCount > 1) {
			nested.set(nested.size() - 1, remainingCount - 1);
			return;
		}

		generator.writeEndArray();
		nested.pop();
		afterElement();
	}

	private static boolean booleanOpt(String opt, boolean dflt, Map<String,Object> options) {
		Object val = options.get(opt);
		if(val instanceof Boolean)
			return (Boolean)val;
		if(val instanceof String)
			return Boolean.valueOf((String)val);
		return dflt;
	}
}
