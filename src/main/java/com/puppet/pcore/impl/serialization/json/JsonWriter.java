package com.puppet.pcore.impl.serialization.json;

import com.puppet.pcore.impl.serialization.AbstractWriter;

import java.util.Map;

public class JsonWriter extends AbstractWriter {
	protected JsonWriter(Map<String,Object> options, JsonPacker packer) {
		super(options, packer);
	}
}
