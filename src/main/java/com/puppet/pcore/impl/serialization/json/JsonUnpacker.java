package com.puppet.pcore.impl.serialization.json;

import com.puppet.pcore.impl.serialization.ExtensionAwareUnpacker;
import com.puppet.pcore.impl.serialization.PayloadReaderFunction;
import com.puppet.pcore.impl.serialization.SerializationException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.puppet.pcore.impl.serialization.json.JsonSerializationFactory.mapper;
import static java.lang.String.format;

public class JsonUnpacker implements ExtensionAwareUnpacker {

	private final Stack<Iterator<?>> etorStack = new Stack<>();
	private final Map<Byte,PayloadReaderFunction<?>> extensionMap = new HashMap<>();

	JsonUnpacker(InputStream in) throws IOException {
		this(mapper.readValue(new BufferedInputStream(in), List.class));
	}

	JsonUnpacker(List<?> values) {
		initialize(values);
	}

	public void initialize(List<?> values) {
		etorStack.push(values.iterator());
	}

	@Override
	public Object read() throws IOException {
		Object obj;
		for(; ; ) {
			Iterator<?> etor = etorStack.lastElement();
			if(etor.hasNext()) {
				obj = etor.next();
				if(obj instanceof Integer)
					obj = ((Integer)obj).longValue();
				else if(obj instanceof Float)
					obj = ((Float)obj).doubleValue();
				break;
			}
			etorStack.pop();
		}

		if(obj instanceof List<?>) {
			Iterator<?> extensionEtor = ((List<?>)obj).iterator();
			if(!extensionEtor.hasNext())
				throw new SerializationException("Unexpected EOF while reading extended data");
			etorStack.push(extensionEtor);
			byte extNo = (byte)readInt();
			PayloadReaderFunction<?> payloadReaderFunction = extensionMap.get(extNo);
			if(payloadReaderFunction == null)
				throw new SerializationException(format("Invalid input. %d is not a valid extension number", extNo));
			obj = payloadReaderFunction.apply(null);
		}
		return obj;
	}

	@Override
	public byte[] readBytes() throws IOException {
		throw new UnsupportedOperationException("readBytes()");
	}

	@Override
	public int readInt() throws IOException {
		Object v = read();
		if(v instanceof Number)
			return ((Number)v).intValue();

		throw new SerializationException(format("Invalid input. Expected integer, got '%s'", v == null ? "null" : v.getClass().getName()));
	}

	@Override
	public long readLong() throws IOException {
		Object v = read();
		if(v instanceof Number)
			return ((Number)v).longValue();

		throw new SerializationException(format("Invalid input. Expected integer, got '%s'", v == null ? "null" : v.getClass().getName()));
	}

	@Override
	public String readString() throws IOException {
		Object v = read();
		if(v instanceof String)
			return (String)v;

		throw new SerializationException(format("Invalid input. Expected string, got '%s'", v == null ? "null" : v.getClass().getName()));
	}

	@Override
	public void registerType(byte extensionNumber, PayloadReaderFunction<?> payloadReaderFunction) {
		extensionMap.put(extensionNumber, payloadReaderFunction);
	}
}
