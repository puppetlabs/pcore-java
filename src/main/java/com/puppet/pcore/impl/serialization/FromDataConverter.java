package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.impl.types.TupleType;
import com.puppet.pcore.impl.types.TypeReferenceType;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.serialization.SerializationException;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.Options.get;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

/**
 * Class that can process the <code>Data</code> produced by the {@link ToDataConverter} class and reassemble
 * the objects that were converted.
 */
public class FromDataConverter implements Converter {
	static class RefEntry {
		private Object value;

		RefEntry() {}

		RefEntry(Object value) {
			setValue(value);
		}

		Object get(Object key) {
			throw new UnsupportedOperationException();
		}

		Object getValue() {
			return value == null ? resolvedEntries() : value;
		}

		Object resolvedEntries() {
			return value;
		}

		void setValue(Object value) {
			if(value instanceof RefEntry)
				throw new IllegalArgumentException();
			this.value = value;
		}

		void update(Object key, RefEntry value) {
			throw new UnsupportedOperationException();
		}
	}

	static class IndexedRefEntry extends RefEntry {
		final List<RefEntry> entries = new ArrayList<>();

		IndexedRefEntry() {
			super();
		}

		IndexedRefEntry(Object value) {
			super(value);
		}

		@Override
		Object get(Object key) {
			return entries.get(((Number)key).intValue());
		}

		@Override
		void update(Object key, RefEntry value) {
			int idx = ((Number)key).intValue();
			if(idx == entries.size())
				entries.add(value);
			else if(idx > entries.size())
				throw new IndexOutOfBoundsException();
			else
				entries.set(idx, value);
		}

		@Override
		Object resolvedEntries() {
			return map(entries, RefEntry::getValue);
		}
	}

	static class KeyedRefEntry extends RefEntry {
		final Map<String, RefEntry> entries = new LinkedHashMap<>();

		KeyedRefEntry() {
			super();
		}

		KeyedRefEntry(Object value) {
			super(value);
		}

		@Override
		Object get(Object key) {
			return entries.get(key);
		}

		Map<String, Object> getEntries() {
			return mapValues(entries, (k, v) -> v.getValue());
		}

		@Override
		Object resolvedEntries() {
			return getEntries();
		}

		@Override
		void update(Object key, RefEntry value) {
			entries.put((String)key, value);
		}
	}

	/**
	 * Converts the given <code>Data</code> <code>value</code> according to the given <code>options</code> and returns
	 * the resulting <code>RichData</code>.
	 *
	 * @param value   the <Data>value</Data> to convert
	 * @param options options hash
	 * @return the processed <code>RichData</code> result
	 */
	public static Object convert(Object value, Map<String,Object> options) {
		return new FromDataConverter(options).convert(value);
	}

	private final boolean allowUnresolved;
	private RefEntry root;
	private RefEntry current;
	private Object currentKey;

	private static final RefEntry NO_VALUE = new RefEntry();

	public FromDataConverter(Map<String, Object> options) {
		allowUnresolved = get(options, "allow_unresolved", false);
		root = NO_VALUE;
	}

	@SuppressWarnings("unchecked")
	private final Map<String,BiFunction<Map<String, Object>, String, Object>> pcoreTypeProcs = asMap(
			entry(PCORE_TYPE_HASH, (Map<String, Object> hash, String typeValue) -> {
				List<Object> value = (List<Object>)hash.get(PCORE_VALUE_KEY);
				return build(new KeyedRefEntry(), () -> {
					int top = value.size();
					for(int idx = 0; idx < top;) {
						Object k = value.get(idx++);
						Object v = value.get(idx++);
						Object key = withoutValue(() -> convert(k));
						with(key, () -> convert(v));
					}
				});
			}),
			entry(PCORE_TYPE_SENSITIVE, (Map<String, Object> hash, String typeValue) -> build(new Sensitive(convert(hash.get(PCORE_VALUE_KEY))))),
			entry(PCORE_TYPE_DEFAULT, (Map<String, Object> hash, String typeValue) -> build(Default.SINGLETON)),
			entry(PCORE_TYPE_SYMBOL, (Map<String, Object> hash, String typeValue) -> build(new Symbol((String)hash.get(PCORE_VALUE_KEY)))),
			entry(PCORE_LOCAL_REF_SYMBOL, (Map<String, Object> hash, String typeValue) -> build(JsonPath.resolve(root, (String)hash.get(PCORE_VALUE_KEY))))
	);

	private Object defaultProc(Map<String, Object> hash, Object typeValue) {
		Object value = hash.containsKey(PCORE_VALUE_KEY) ? hash.get(PCORE_VALUE_KEY) : reject(hash, (e) -> e.getKey().equals(PCORE_TYPE_KEY));
		if(typeValue instanceof Map<?, ?>) {
			Object type = withoutValue(() -> convert(typeValue));
			if(type instanceof Map) {
				if(allowUnresolved)
					return hash;
				throw new SerializationException(format("Unable to deserialize type from %s", type));
			}
			return pcoreTypeHashToValue((AnyType)type, value);
		}
		if(typeValue instanceof String) {
			AnyType type = (AnyType)Pcore.typeEvaluator().resolveType((String)typeValue);
			if(type instanceof TypeReferenceType) {
				if(allowUnresolved)
					return hash;
				throw new SerializationException(format("No implementation mapping found for Puppet Type %s", typeValue));
			}
			return pcoreTypeHashToValue(type, value);
		}
		throw new SerializationException(format("Cannot parse a type from %s", typeValue));
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object value) {
		if(value instanceof Map<?, ?>) {
			Map<String, Object> hash = (Map<String,Object>)value;
			Object pcoreType = hash.get(PCORE_TYPE_KEY);
			if(pcoreType instanceof String) {
				BiFunction<Map<String, Object>, String, Object> func = pcoreTypeProcs.get(pcoreType);
				return func == null ? defaultProc(hash, pcoreType) : func.apply(hash, (String)pcoreType);
			}

			return pcoreType != null
				? defaultProc(hash, pcoreType)
			  : build(new KeyedRefEntry(), () -> hash.forEach((k, v) -> with(k, () -> convert(v))));
		}

		return value instanceof List<?>
			? build(new IndexedRefEntry(), () -> eachWithIndex((List<?>)value, (v, i) -> with(i, () -> convert(v))))
		  : build(numericConvert(value));
	}

	private static Object numericConvert(Object value) {
		if(value instanceof Number) {
			Number n = (Number)value;
			if(n instanceof Float || n instanceof Double)
				return n.doubleValue();

			return n.longValue();
		}
		return value;
	}

	private <T> T build(T value) {
		if(current != null)
			current.update(currentKey, new RefEntry(value));
		return value;
	}

	@SuppressWarnings("unchecked")
	private <T> T build(RefEntry value, Runnable block) {
		if(current != null)
			current.update(currentKey, value);
		withValue(value, block);
		return (T)value.getValue();
	}

	@SuppressWarnings("unchecked")
	private void withValue(RefEntry value, Runnable block) {
		if(root == NO_VALUE)
			root = value;
		RefEntry parent = current;
		current = value;
		block.run();
		current = parent;
	}

	private void with(Object key, Runnable block) {
		Object parentKey = currentKey;
		currentKey = key;
		block.run();
		currentKey = parentKey;
	}

	private <T> T withoutValue(Supplier<T> supplier) {
		RefEntry parent = current;
		current = null;
		T value = supplier.get();
		current = parent;
		return value;
	}

	class ConverterAttributeAccessor implements ArgumentsAccessor {
		private final AnyType type;
		private final Map<String, Object> initHash;
		private KeyedRefEntry converted;
		private boolean remembered = false;

		ConverterAttributeAccessor(AnyType type, Map<String, Object> initHash) {
			this.type = type;
			this.initHash = initHash;
		}

		@Override
		public Object get(int index) {
			if(index != 0)
				throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
			if(converted == null) {
				if(remembered) {
					initHash.forEach((key, value) -> with(key, () -> convert(value)));
					converted = (KeyedRefEntry)current;
				} else {
					Object key = currentKey;
					Object curr = current;
					converted = new KeyedRefEntry();
					build(converted, () -> initHash.forEach((k, v) -> with(k, () -> convert(v))));
					if(curr instanceof List<?> && key instanceof Number)
						((List)curr).remove(((Number)key).intValue());
				}
			}
			return converted.getEntries();
		}

		@Override
		public Object[] getAll() {
			return new Object[] { get(0) };
		}

		@Override
		public List<Object> getArgumentList() {
			return singletonList(get(0));
		}

		@Override
		public TupleType getParametersType() {
			return tupleType(singletonList(hashType(stringType(), anyType())));
		}

		@Override
		public AnyType getType() {
			return type;
		}

		@Override
		public <T> T remember(T createdInstance) {
			if(converted != null)
				converted.setValue(createdInstance);
			else {
				KeyedRefEntry instanceAndHash = new KeyedRefEntry(createdInstance);
				if(current != null)
					current.update(currentKey, instanceAndHash);
				current = instanceAndHash;
				if(root == NO_VALUE)
					root = current;
			}
			remembered = true;
			return createdInstance;
		}

		@Override
		public int size() {
			return 1;
		}
	}

	@SuppressWarnings("unchecked")
	private Object pcoreTypeHashToValue(AnyType pcoreType, Object value) {
		if(value instanceof Map<?, ?>) {
			// Complex object
			FactoryDispatcher<?> factoryDispatcher = pcoreType.factoryDispatcher();
			Map<String, Object> hash = (Map<String, Object>)value;
			if(hash.isEmpty())
				return build(factoryDispatcher.createInstance(pcoreType));

			try {
				RefEntry parent = current;
				Object created = factoryDispatcher.createInstance(pcoreType, new ConverterAttributeAccessor(pcoreType, hash));
				current = parent;
				return created;
			} catch(IOException e) {
				throw new PcoreException(e);
			}
		}

		if(value instanceof String)
			return build(pcoreType.newInstance(value));

		throw new SerializationException(format("Cannot create a %s from a %s", pcoreType.name(), value.getClass().getName()));
	}
}
