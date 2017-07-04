package com.puppet.pcore.impl;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.types.*;
import com.puppet.pcore.serialization.Constructor;
import com.puppet.pcore.serialization.FactoryFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Represents the constructor for a type.
 * @param <T>
 */
public class ConstructorImpl<T> implements Constructor<T> {
	private final TupleType signature;
	private final FactoryFunction<T> initFunction;

	public static <T> ConstructorImpl<T> constructor(ObjectType type) {
		ParameterInfo pi = type.parameterInfo();
		List<ObjectType.Attribute> attrs = pi.attributes;
		int count = attrs.size();
		List<StructElement> elems = new ArrayList<>(count);
		for(int idx = 0; idx < count; ++idx) {
			ObjectType.Attribute attr = attrs.get(idx);
			elems.add(idx >= pi.requiredCount
				? structElement(optionalType(attr.name), attr.type)
				: structElement(attr.name, attr.type));
		}

		return hashConstructor(argValues -> {
			Map<String,Object> hash = (Map<String,Object>)argValues.get(0);
			int top = attrs.size();
			Object[] args = new Object[top];
			for(int idx = 0; idx < top; ++idx) {
				ObjectType.Attribute attr = attrs.get(idx);
				Object val = hash.get(attr.name);
				if(val == null && !hash.containsKey(attr.name)) {
					// No value supplied for the given name
					if(!attr.hasValue())
						throw new IllegalArgumentException(format("Missing required key %s in init hash for %s", attr.name, type.name()));
					val = attr.value();
				}
				args[idx] = val;
			}
			return (T)type.newInstance(args);
		}, structType(elems));
	}

	public static <T> ConstructorImpl<T> constructor(FactoryFunction<T> initFunction, AnyType...paramTypes) {
		return constructor(initFunction, tupleType(asList(paramTypes)));
	}

	public static <T> ConstructorImpl<T> constructor(FactoryFunction<T> initFunction, TupleType paramTypes) {
		return new ConstructorImpl<T>(paramTypes, initFunction);
	}

	public static <T> ConstructorImpl<T> hashConstructor(FactoryFunction<T> initFunction, StructType hashType) {
		return new HashConstructor<T>(hashType, initFunction);
	}

	ConstructorImpl(TupleType signature, FactoryFunction<T> initFunction) {
		this.signature = signature;
		this.initFunction = initFunction;
	}

	@Override
	public FactoryFunction<T> initFunction() {
		return initFunction;
	}

	public boolean isHashConstructor() {
		return false;
	}

	@Override
	public TupleType signature() {
		return signature;
	}
}

class HashConstructor<T> extends ConstructorImpl<T> {
	HashConstructor(StructType hashType, FactoryFunction<T> initFunction) {
		super(tupleType(singletonList(hashType)), initFunction);
	}

	public boolean isHashConstructor() {
		return true;
	}
}
