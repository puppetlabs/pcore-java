package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.Default;
import com.puppet.pcore.Pcore;
import com.puppet.pcore.Sensitive;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Constants;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.serialization.extension.*;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;
import com.puppet.pcore.serialization.Deserializer;
import com.puppet.pcore.serialization.Reader;
import com.puppet.pcore.serialization.SerializationException;

import java.io.IOException;
import java.util.*;

public class DeserializerImpl implements Deserializer {
	private final List<Object> objectsRead = new ArrayList<>();
	private final Reader reader;

	public DeserializerImpl(Reader reader) {
		this.reader = reader;
	}

	@Override
	public Reader getReader() {
		return reader;
	}

	@Override
	public Object read() throws IOException {
		Object val = reader.read();
		if(val instanceof Tabulation)
			return objectsRead.get(((Tabulation)val).index);

		if(val == null || val instanceof Number || val instanceof String || val instanceof Boolean || val instanceof
				Default)
			return val;

		if(val instanceof MapStart) {
			int top = ((MapStart)val).size * 2;
			Object[] keyValuePairs = new Object[top];
			Map<Object,Object> result = remember(Helpers.asWrappingMap(keyValuePairs));
			for(int idx = 0; idx < top;) {
				keyValuePairs[idx++] = read();
				keyValuePairs[idx++] = read();
			}
			return result;
		}

		if(val instanceof ArrayStart) {
			final int top = ((ArrayStart)val).size;
			Object[] values = new Object[top];
			List<Object> result = remember(Helpers.asWrappingList(values));
			for(int idx = 0; idx < top; ++idx)
				values[idx] = read();
			return result;
		}

		if(val instanceof SensitiveStart)
			return new Sensitive(read());

		if(val instanceof PcoreObjectStart) {
			PcoreObjectStart os = (PcoreObjectStart)val;
			Type type = Pcore.typeEvaluator().resolveType(os.typeName);
			if(!(type instanceof ObjectType))
				throw new SerializationException("No implementation mapping found for Puppet Type " + os.typeName);

			ObjectType ot = (ObjectType)type;
			val = ot.newInstance(new DeserializerArgumentsAccessor(this, ot, os.attributeCount));
			if(val instanceof ObjectType) {
				TypedName tn = new TypedName(Constants.KEY_TYPE, ((ObjectType)val).name().toLowerCase());

				// Add result to the loader unless it is the exact same instance as the type returned from loadOrNull. The add
				// will succeed when loadOrNull returns null.
				Loader loader = Pcore.loader();
				if(val != loader.loadOrNull(tn))
					loader.bind(tn, val);
			}
			return val;
		}

		if(val instanceof ObjectStart) {
			ObjectStart os = (ObjectStart)val;
			ObjectType ot = (ObjectType)read();
			return ot.newInstance(new DeserializerArgumentsAccessor(this, ot, os.attributeCount - 1));
		}
		return remember(val);
	}

	<T> T remember(T value) {
		objectsRead.add(value);
		return value;
	}

	<T> void replacePlaceHolder(Object placeHolder, T createdInstance) {
		int idx = objectsRead.size();
		while(--idx >= 0) {
			if(objectsRead.get(idx) == placeHolder) {
				objectsRead.set(idx, createdInstance);
				return;
			}
		}
		throw new IllegalArgumentException("Attempt to replace non-existent place-holder");
	}
}
