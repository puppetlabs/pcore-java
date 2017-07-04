package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.StringConverter;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.impl.types.ParameterInfo;
import com.puppet.pcore.impl.types.RuntimeType;
import com.puppet.pcore.serialization.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.Options.get;
import static com.puppet.pcore.impl.types.TypeFactory.infer;
import static com.puppet.pcore.impl.types.TypeFactory.scalarDataType;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;

/**
 * Class that can process an arbitrary object into a value that is assignable to <code>Data</code>.
 */
public class ToDataConverter implements Converter {
	public static Object convert(Object value, Map<String,Object> options) {
		return new ToDataConverter(options).convert(value);
	}

	private final Logger logger = LoggerFactory.getLogger(ToDataConverter.class);
	private final boolean typeByReference;
	private final boolean localReference;
	private final boolean symbolAsString;
	private final boolean richData;
	private final String messagePrefix;

	private List<Object> path;
	private Map<Object,Object> values;
	private Map<Object,Boolean> recursiveLock;

	public ToDataConverter(Map<String,Object> options) {
		typeByReference = get(options, "typeByReference", true);
		localReference = get(options, "localReference", true);
		symbolAsString = get(options, "symbolAsString", false);
		richData = get(options, "richData", true);
		messagePrefix = get(options, "messagePrefix", "");
	}

	public Object convert(Object value) {
		path = new ArrayList<>();
		values = new IdentityHashMap<>();
		return toData(value);
	}

	private String pathToString() {
		StringBuilder bld = new StringBuilder();
		if(messagePrefix != null)
			bld.append(messagePrefix);
		bld.append(JsonPath.toJsonPath(path));
		return bld.toString();
	}

	private Object toData(Object value) {
		if(value == null || scalarDataType().isInstance(value))
			return value;

		if(value instanceof Symbol)
			return symbolAsString ? value.toString() : asMap(PCORE_TYPE_KEY, PCORE_TYPE_SYMBOL, PCORE_VALUE_KEY, value.toString());

		if(value instanceof Default)
			return singletonMap(PCORE_TYPE_KEY, PCORE_TYPE_DEFAULT);

		if(value instanceof List<?>)
			return process(value, () -> mapWithIndex((List<Object>)value, (Object v, Integer i) -> with(i, () -> toData(v))));

		if(value instanceof Map<?, ?>) {
			Map<?,?> mapValue = (Map<?,?>)value;
			return process(mapValue, () ->
					all(mapValue.keySet(), (key) -> key instanceof String)
						? mapValues(mapValue, (Object key, Object v) -> with(key, () -> toData(v)))
						: nonKeyKeyedHashToData((mapValue)));
		}

		if(value instanceof Sensitive)
			return process(value, () -> asMap(PCORE_TYPE_KEY, PCORE_TYPE_SENSITIVE, PCORE_VALUE_KEY, toData(((Sensitive)value).unwrap())));

		return unknownToData(value);
	}

	private Object unknownToData(Object value) {
		return richData ? valueToDataHash(value) : unknownToStringWithWarning(value);
	}

	private Object valueToDataHash(Object value) {
		AnyType pcoreType = value instanceof PuppetObject ? (AnyType)((PuppetObject)value)._pcoreType() : infer(value);
		if(pcoreType instanceof RuntimeType)
			return unknownToStringWithWarning(value);

		Object pcoreTv = pcoreTypeToData(pcoreType);
		if(pcoreType.roundtripWithString())
			return asMap(PCORE_TYPE_KEY, pcoreTv, PCORE_VALUE_KEY, StringConverter.singleton.convert(value));

		if(value instanceof PuppetObjectWithHash) {
			return process(value, () -> {
				Map<Object,Object> result = new LinkedHashMap<>();
				result.put(PCORE_TYPE_KEY, pcoreTv);
				result.putAll((Map<?,?>)toData(((PuppetObjectWithHash)value)._pcoreInitHash()));
				return result;
			});
		}

		if(pcoreType instanceof ObjectType) {
			ObjectType ot = (ObjectType)pcoreType;
			Object[] args = ot.attributeValuesFor(value);
			ParameterInfo pi = ot.parameterInfo();

			return process(value, () -> {
				Map<Object,Object> result = new LinkedHashMap<>();
				List<ObjectType.Attribute> attrs = pi.attributes;
				result.put(PCORE_TYPE_KEY, pcoreTv);
				for(int idx = 0; idx < args.length; ++idx) {
					String k = attrs.get(idx).name;
					Object v = args[idx];
					with(k, () -> result.put(k, toData(v)));
				}
				return result;
			});
		}

		throw new SerializationException(format("No Puppet Type found for %s", value.getClass().getName()));
	}

	private Object pcoreTypeToData(AnyType pcoreType) {
		String typeName = pcoreType.name();
		return (typeByReference || typeName.startsWith(("Pcore::"))) ? typeName : with(PCORE_TYPE_KEY, () -> toData(pcoreType));
	}

	private Object nonKeyKeyedHashToData(Map<?, ?> hash) {
		if(richData)
			return toKeyExtendedHash(hash);

		return map(hash, (Object k, Object v) -> {
			String key = symbolAsString && k instanceof Symbol ? k.toString() : unknownKeyToStringWithWarning(k);
			return entry(key, with(key, () -> toData(v)));
		});
	}

	private String unknownToStringWithWarning(Object value) {
		String str = value.toString();
		if(logger.isWarnEnabled())
			logger.warn("{} contains {} value. It will be converted to the String '{}'", pathToString(), value.getClass().getName(), str);
		return str;
	}

	private String unknownKeyToStringWithWarning(Object key) {
		String str = key.toString();
		if(logger.isWarnEnabled())
			logger.warn("{} contains a hash with {} key. It will be converted to the String '{}'", pathToString(), key.getClass().getName(), str);
		return str;
	}

	private Map<Object, Object> toKeyExtendedHash(Map<?, ?> hash) {
		List<Object> pairs = new ArrayList<>();
		for(Entry<?, ?> e : hash.entrySet()) {
			Object key = toData(e.getKey());
			pairs.add(key);
			pairs.add(with(key, () -> toData(e.getValue())));
		}
		return asMap(PCORE_TYPE_KEY, PCORE_TYPE_HASH, PCORE_VALUE_KEY, pairs);
	}

	private Object process(Object value, Supplier<Object> block) {
		if(!localReference)
			return withRecursionGuard(value, block);

		Object ref = values.get(value);
		if(ref == null) {
			values.put(value, new ArrayList<>(path));
			return block.get();
		}

		if(ref instanceof Map<?,?>)
			return ref;

		String jsonRef = JsonPath.toJsonPath((List<?>)ref);
		if(jsonRef == null)
			// Complex key and hence no way to reference the prior value. The value must therefore be
      // duplicated which in turn introduces a risk for endless recursion in case of self
      // referencing structures
			return withRecursionGuard(value, block);

		Map<String, Object> refMap = asMap(PCORE_TYPE_KEY, PCORE_LOCAL_REF_SYMBOL, PCORE_VALUE_KEY, jsonRef);
		values.put(value, refMap);
		return refMap;
	}


	private Object with(Object key, Supplier<Object> block) {
		path.add(key);
		Object value = block.get();
		path.remove(path.size() - 1);
		return value;
	}

	private Object withRecursionGuard(Object value, Supplier<Object> block) {
		if(recursiveLock != null) {
			if(recursiveLock.put(value, Boolean.TRUE) == Boolean.TRUE)
				throw new SerializationException(format("Endless recursion when serializing instance of %s", value.getClass().getName()));
		} else {
			recursiveLock = new IdentityHashMap<>();
			recursiveLock.put(value, Boolean.TRUE);
		}
		Object result = block.get();
		recursiveLock.remove(value);
		return result;
	}
}
