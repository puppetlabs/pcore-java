package com.puppet.pcore.impl.serialization.json;

import com.puppet.pcore.impl.serialization.AbstractReader;

import java.util.List;

public class JsonReader extends AbstractReader {
	@Override
	public void initialize(List<?> data) {
		((JsonUnpacker)unpacker).initialize(data);
	}

	protected JsonReader(JsonUnpacker unpacker) {
		super(unpacker);
	}
}
