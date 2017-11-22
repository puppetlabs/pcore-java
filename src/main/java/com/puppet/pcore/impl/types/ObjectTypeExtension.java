package com.puppet.pcore.impl.types;

import com.puppet.pcore.Default;
import com.puppet.pcore.PcoreException;
import com.puppet.pcore.impl.Assertions;
import com.puppet.pcore.impl.eval.CompareOperator;
import com.puppet.pcore.parser.ParseException;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.impl.types.ObjectType.Attribute;
import com.puppet.pcore.impl.types.ObjectType.TypeParameter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class ObjectTypeExtension extends AnyType {
	public final ObjectType baseType;
	public final Map<String,Object> parameters;

	private static Map<String,Object> parametersFromInstance(ObjectType baseType, Object instance) {
		Map<String,TypeParameter> pts = baseType.typeParameters(true);
		Map<String,Attribute> attrs = baseType.attributes(true);
		Map<String,Object> parameters = new LinkedHashMap<>(pts.size());
		for(String key : pts.keySet()) {
			Attribute attr = attrs.get(key);
			parameters.put(key, attr == null ? null : attr.get(instance));
		}
		return parameters;
	}

	private static Map<String,Object> parametersFromList(ObjectType baseType, List<Object> parameters) {
		if(parameters.isEmpty())
			return emptyMap();

		Map<String,TypeParameter> pts = baseType.typeParameters(true);
		if(parameters.size() == 1) {
			Object first = parameters.get(0);
			if(first instanceof Map<?,?>) {
				if(pts.size() == 1) {
					Entry<String,TypeParameter> entry = pts.entrySet().iterator().next();
					if(entry.getValue().type.isInstance(first)) {
						return Collections.singletonMap(entry.getKey(), first);
					}
				}
				return (Map<String,Object>)first;
			}
		}

		int idx = 0;
		int top = parameters.size();
		Map<String,Object> paramMap = new LinkedHashMap<>(top);
		for(Entry<String,TypeParameter> entry : pts.entrySet()) {
			if(idx >= top)
				break;
			paramMap.put(entry.getKey(), parameters.get(idx++));
		}
		return paramMap;
	}

	public ObjectTypeExtension(ObjectType baseType, Object instance) {
		this(baseType, parametersFromInstance(baseType, instance));
	}

	public ObjectTypeExtension(ObjectType baseType, List<Object> parameters) {
		this(baseType, parametersFromList(baseType, parameters));
	}

	private ObjectTypeExtension(ObjectType baseType, Map<String,Object> parameters) {
		Map<String,TypeParameter> pts = baseType.typeParameters(true);
		if(pts.isEmpty())
			throw new ParseException(format("The %s-Type cannot be parameterized using []", baseType.name()));

		Map<String,Object> paramsCopy = new LinkedHashMap<>(parameters);
		for(Entry<String,Object> entry : parameters.entrySet()) {
			TypeParameter tp = pts.get(entry.getKey());
			if(tp == null)
				throw new ParseException(format("'%s' is not a known type parameter for %s-Type", entry.getKey(), baseType.name()));

			Object v = entry.getValue();
			if(v == Default.SINGLETON)
				paramsCopy.remove(entry.getKey());
			else
				Assertions.assertInstance(tp.type, v, () -> tp.label());
		}
		if(paramsCopy.isEmpty())
			throw new ParseException(format("The %s-Type cannot be parameterized using an empty parameter list", baseType.name()));

		this.baseType = baseType;
		this.parameters = unmodifiableMap(paramsCopy);
	}

	public FactoryDispatcher<?> factoryDispatcher() {
		return baseType.factoryDispatcher();
	}

	public AnyType generalize() {
		return baseType;
	}

	public int hashCode() {
		return baseType.hashCode() ^ parameters.hashCode();
	}

	public Object initParameters() {
		Map<String,TypeParameter> pts = baseType.typeParameters(true);
		if(pts.size() > 2)
			return parameters;

		List<Object> result = new ArrayList();
		for(TypeParameter tp : pts.values()) {
			String pn = tp.name;
			result.add(parameters.containsKey(pn) ? parameters.get(pn) : Default.SINGLETON);
		}
		while(result.size() > 1 && result.get(result.size() - 1) == Default.SINGLETON)
			result.remove(result.size() - 1);
		return result;
	}

	public boolean isEqualityIncludeType() {
		return baseType.isEqualityIncludeType();
	}

	public boolean isInstance(Object value, RecursionGuard guard) {
		return baseType.isInstance(value, guard) && testInstance(value, guard);
	}

	public boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof ObjectTypeExtension) {
			ObjectTypeExtension ot = (ObjectTypeExtension)type;
			return baseType.isAssignable(ot.baseType, guard) && testAssignable(ot.parameters, guard);
		}
		return baseType.isAssignable(type) && testAssignable(emptyMap(), guard);
	}

	public String name() {
		return baseType.name();
	}

	@Override
	public Object newInstance(Object... args) {
		return baseType.newInstance(args);
	}

	@Override
	public Object newInstance(ArgumentsAccessor aa) throws IOException {
		return baseType.newInstance(aa);
	}

	public ParameterInfo parameterInfo() {
		return baseType.parameterInfo();
	}

	protected boolean testInstance(Object instance, RecursionGuard guard) {
		return all(parameters.entrySet(), (e) -> match(baseType.getAttribute(e.getKey()).get(instance), e.getValue()));
	}

	protected boolean testAssignable(Map<String,Object> paramValues, RecursionGuard guard) {
		return all(parameters.entrySet(), (e) -> {
			Object a = e.getValue();
			Object b = paramValues.get(e.getKey());
			return match(b, a) || a instanceof AnyType && b instanceof AnyType && ((AnyType)a).isAssignable((AnyType)b, guard);
		});
	}

	protected boolean match(Object a, Object b) {
		return CompareOperator.match(a, b, null);
	}

	boolean guardedEquals(Object v, RecursionGuard guard) {
		if(v instanceof ObjectTypeExtension) {
			ObjectTypeExtension ov = (ObjectTypeExtension)v;
			return baseType.guardedEquals(((ObjectTypeExtension)v).baseType, guard) && parameters.equals(ov.parameters);
		}
		return false;
	}
}
