package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.DynamicObjectImpl;
import com.puppet.pcore.impl.serialization.extension.*;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.impl.types.TypeReferenceType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.Serializer;
import com.puppet.pcore.serialization.Writer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class SerializerImpl implements Serializer {
	private final Map<Object,Integer> objectsWritten = new IdentityHashMap<>();
	private final Pcore pcore;
	private final Writer writer;

	public SerializerImpl(Pcore pcore, Writer writer) {
		this.pcore = pcore;
		this.writer = writer;
	}

	@Override
	public void finish() throws IOException {
		writer.finish();
	}

	public void startPcoreObject(String typeName, int attributeCount) throws IOException {
		writer.write(new PcoreObjectStart(typeName, attributeCount));
	}

	public void startObject(int attributeCount) throws IOException {
		writer.write(new ObjectStart(attributeCount));
	}

	@Override
	public void write(Object value) throws IOException {
		if(value == null || value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Default)
			writer.write(value);
		else {
			Integer index = objectsWritten.get(value);
			if(index == null)
				writeTabulatedFirstTime(value);
			else
				writer.write(new Tabulation(index));
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void writeObject(T value) throws IOException {
		Function<T,Object[]> attributeProvider;
		Type type = value instanceof PObject ? ((PObject)value)._pType() : pcore.infer(value);
		if(value instanceof DynamicObjectImpl) {
			attributeProvider = t -> ((DynamicObjectImpl)value).getAttributes();
		} else {
			Class<T> implClass = (Class<T>)value.getClass();
			if(!(type instanceof ObjectType))
				throw new SerializationException(format("No Puppet Type found for %s", implClass.getName()));

			attributeProvider = pcore.implementationRegistry().attributeProviderFor(implClass);
			if(attributeProvider == null)
				throw new SerializationException(format("No Object Writer found for %s", implClass.getName()));
		}

		Object[] args = attributeProvider.apply(value);
		ObjectType.ParameterInfo pi = ((ObjectType)type).parameterInfo();

		// Limit the write to not include trailing defaults
		int top = args.length;
		while(--top >= 0) {
			ObjectType.Attribute attr = pi.attributes.get(top);
			if(!(attr.hasValue() && Objects.equals(attr.value(), args[top])))
				break;
		}
		++top;

		if(type.name().startsWith("Pcore::")) {
			objectsWritten.put(value, objectsWritten.size());
			startPcoreObject(type.name(), top);
		} else {
			startObject(top + 1);
			write(type);
			objectsWritten.put(value, objectsWritten.size());
		}
		for(int idx = 0; idx < top; ++idx)
			write(args[idx]);
	}

	private void writeTabulatedFirstTime(Object value) throws IOException {
		if(value instanceof Symbol
				|| value instanceof Pattern
				|| value instanceof Version
				|| value instanceof VersionRange
				|| value instanceof Duration
				|| value instanceof Instant
				|| value instanceof Binary
				|| value instanceof TypeReferenceType) {
			objectsWritten.put(value, objectsWritten.size());
			writer.write(value);
		} else if(value instanceof List<?>) {
			List<?> lv = (List<?>)value;
			objectsWritten.put(value, objectsWritten.size());
			writer.write(new ArrayStart(lv.size()));
			for(Object v : lv)
				write(v);
		} else if(value instanceof Map<?,?>) {
			Map<?,?> mv = (Map<?,?>)value;
			objectsWritten.put(value, objectsWritten.size());
			writer.write(new MapStart(mv.size()));
			for(Map.Entry<?,?> me : mv.entrySet()) {
				write(me.getKey());
				write(me.getValue());
			}
		} else if(value instanceof Sensitive) {
			objectsWritten.put(value, objectsWritten.size());
			writer.write(SensitiveStart.SINGLETON);
			write(((Sensitive)value).unwrap());
		} else {
			writeObject(value);
		}
	}
}
