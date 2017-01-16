package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Comment;
import com.puppet.pcore.Default;
import com.puppet.pcore.Symbol;
import com.puppet.pcore.impl.serialization.extension.*;
import com.puppet.pcore.impl.types.TypeReferenceType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.Writer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.splitName;
import static com.puppet.pcore.impl.serialization.extension.Numbers.*;

public abstract class AbstractWriter implements Writer {
	private final Map<Object,Integer> objectsWritten = new HashMap<>();
	private final ExtensionAwarePacker packer;
	private final boolean tabulate;

	protected AbstractWriter(Map<String,Object> options, ExtensionAwarePacker packer) {
		this.packer = packer;
		Object tabulate = options.get("tabulate");
		if(tabulate instanceof Boolean)
			this.tabulate = (Boolean)tabulate;
		else if(tabulate instanceof String)
			this.tabulate = Boolean.valueOf((String)tabulate);
		else
			this.tabulate = true;
		registerTypes();
	}

	@Override
	public void finish() throws IOException {
		packer.flush();
	}

	@Override
	public void write(Object value) throws IOException {
		if(!tabulate || value == null || value instanceof Number || value instanceof Boolean || value instanceof Symbol ||
				value instanceof NotTabulated) {
			// Not tabulated
			packer.write(value);
			return;
		}

		Integer index = objectsWritten.get(value);
		if(index == null) {
			packer.write(value);
			objectsWritten.put(value, objectsWritten.size());
		} else
			packer.write(new InnerTabulation(index));
	}

	protected byte[] buildPayload(IOConsumer<ExtensionAwarePacker> consumer) throws IOException {
		consumer.accept(packer);
		return null;
	}

	protected boolean supportsBinary() {
		return false;
	}

	private <T> void registerType(byte extensionNumber, Class<T> extClass, PayloadWriterFunction<T> block) {
		packer.registerType(extensionNumber, extClass, block);
	}

	private void registerTypes() {
		registerType(INNER_TABULATION, InnerTabulation.class, o -> buildPayload(ep -> ep.write(o.index)));
		registerType(TABULATION, Tabulation.class, o -> buildPayload(ep -> ep.write(o.index)));
		registerType(ARRAY_START, ArrayStart.class, o -> buildPayload(ep -> ep.write(o.size)));
		registerType(MAP_START, MapStart.class, o -> buildPayload(ep -> ep.write(o.size)));
		registerType(PCORE_OBJECT_START, PcoreObjectStart.class, o -> buildPayload(ep -> {
			writePayloadQName(ep, o.typeName);
			ep.write(o.attributeCount);
		}));
		registerType(OBJECT_START, ObjectStart.class, o -> buildPayload(ep -> ep.write(o.attributeCount)));
		registerType(SENSITIVE_START, SensitiveStart.class, o -> buildPayload(ep -> {}));
		registerType(DEFAULT, Default.class, o -> buildPayload(ep -> {}));
		registerType(COMMENT, Comment.class, o -> buildPayload(ep -> ep.write(o.comment)));
		registerType(REGEXP, Pattern.class, o -> buildPayload(ep -> ep.write(o.pattern())));
		registerType(TYPE_REFERENCE, TypeReferenceType.class, o -> buildPayload(ep -> ep.write(o.typeString)));
		registerType(SYMBOL, Symbol.class, o -> buildPayload(ep -> ep.write(o.toString())));
		registerType(TIME, Instant.class, o -> buildPayload(ep -> {
			ep.write(o.getEpochSecond());
			ep.write(o.getNano());
		}));
		registerType(TIMESPAN, Duration.class, o -> buildPayload(ep -> {
			ep.write(o.getSeconds());
			ep.write(o.getNano());
		}));
		registerType(VERSION, Version.class, o -> buildPayload(ep -> ep.write(o.toString())));
		registerType(VERSION_RANGE, VersionRange.class, o -> buildPayload(ep -> ep.write(o.toString())));
		if(supportsBinary())
			registerType(BINARY, Binary.class, o -> buildPayload(ep -> ep.write(o.toByteArray())));
		else
			registerType(BASE64, Binary.class, o -> buildPayload(ep -> ep.write(o.toString())));
	}

	private void writePayloadQName(ExtensionAwarePacker ep, String qname) throws IOException {
		String[] segments = splitName(qname);
		ep.write(segments.length);
		for(String segment : segments)
			writePayloadString(ep, segment);
	}

	private void writePayloadString(ExtensionAwarePacker ep, String string) throws IOException {
		Object value = string;
		if(tabulate) {
			Integer index = objectsWritten.get(value);
			if(index == null)
				objectsWritten.put(value, objectsWritten.size());
			else
				value = index;
		}
		ep.write(value);
	}
}
