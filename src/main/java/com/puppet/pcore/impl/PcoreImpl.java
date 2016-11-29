package com.puppet.pcore.impl;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.loader.BasicLoader;
import com.puppet.pcore.impl.serialization.SerializationException;
import com.puppet.pcore.impl.serialization.json.JsonSerializationFactory;
import com.puppet.pcore.impl.serialization.msgpack.MsgPackSerializationFactory;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.impl.types.TypeFactory;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;
import com.puppet.pcore.serialization.FactoryFunction;
import com.puppet.pcore.serialization.SerializationFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

import static com.puppet.pcore.impl.Constants.*;
import static com.puppet.pcore.impl.types.TypeFactory.objectType;
import static com.puppet.pcore.impl.types.TypeFactory.typeReferenceType;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class PcoreImpl implements Pcore {
	private final ImplementationRegistry implementationRegistry = new ImplementationRegistryImpl();
	private final Loader loader = new BasicLoader();
	private final TypeEvaluator defaultEvaluator = new TypeEvaluatorImpl(loader);

	public PcoreImpl() {
		try {
			Collection<AnyType> basicTypes = TypeEvaluatorImpl.BASIC_TYPES.values();
			List<ObjectType> metaTypes = new ArrayList<>(basicTypes.size());
			for(AnyType type : basicTypes) {
				Method registerPtypeMethod = type.getClass().getDeclaredMethod("registerPcoreType", PcoreImpl.class);
				registerPtypeMethod.setAccessible(true);
				metaTypes.add((ObjectType)registerPtypeMethod.invoke(null, this));
			}
			for(ObjectType metaType : metaTypes)
				metaType.resolve(defaultEvaluator);
		} catch(NoSuchMethodException | IllegalAccessException e) {
			throw new PCoreException(e);
		} catch(InvocationTargetException e) {
			throw new PCoreException(e.getTargetException());
		}
	}

	public <T> ObjectType createObjectType(
			Class<T> implClass, String typeName, String parentName, FactoryFunction<T>
			creator) {
		return createObjectType(implClass, typeName, parentName, emptyMap(), creator);
	}

	public <T> ObjectType createObjectType(
			Class<T> implClass, String typeName, String parentName, Map<String,Object>
			attributesHash, FactoryFunction<T> creator) {
		return createObjectType(implClass, typeName, parentName, attributesHash, creator, (self) -> EMPTY_ARRAY);
	}

	public <T> ObjectType createObjectType(
			Class<T> implClass, String typeName, String parentName, Map<String,Object>
			attributesHash, FactoryFunction<T> creator, Function<T,Object[]> attributeSupplier) {
		return createObjectType(implClass, typeName, parentName, attributesHash, emptyList(), creator, attributeSupplier);
	}

	public <T> ObjectType createObjectType(
			Class<T> implClass, String typeName, String parentName, Map<String,Object>
			attributesHash, List<String> serialization, FactoryFunction<T> creator, Function<T,Object[]> attributeSupplier) {
		return createObjectType(implClass, typeName, parentName, attributesHash, emptyMap(), emptyList(), serialization, creator, attributeSupplier);
	}

	public <T> ObjectType createObjectType(
			Class<T> implClass, String typeName, String parentName, Map<String,Object>
			attributesHash, Map<String,Object> functionsHash, List<String> equality, List<String> serialization,
			FactoryFunction<T> creator, Function<T,
			Object[]> attributeSupplier) {
		Map<String,Object> i12nHash = new HashMap<>();
		i12nHash.put(KEY_NAME, typeName);
		if(parentName != null)
			i12nHash.put(KEY_PARENT, typeReferenceType(parentName));
		if(!attributesHash.isEmpty())
			i12nHash.put(KEY_ATTRIBUTES, attributesHash);
		if(!functionsHash.isEmpty())
			i12nHash.put(KEY_FUNCTIONS, functionsHash);
		if(!equality.isEmpty())
			i12nHash.put(KEY_EQUALITY, equality);
		if(!serialization.isEmpty())
			i12nHash.put(KEY_SERIALIZATION, serialization);
		implementationRegistry.registerImplementation(typeName, implClass.getName(), creator, attributeSupplier);
		ObjectType type = objectType(i12nHash);
		loader.bind(new TypedName("type", typeName), type);
		return type;
	}

	@Override
	public ImplementationRegistry implementationRegistry() {
		return implementationRegistry;
	}

	@Override
	public Type infer(Object value) {
		return TypeFactory.infer(value);
	}

	@Override
	public Type inferSet(Object value) {
		return TypeFactory.inferSet(value);
	}

	@Override
	public SerializationFactory serializationFactory(String serializationFormat) {
		switch(serializationFormat) {
		case SerializationFactory.MSGPACK:
			return new MsgPackSerializationFactory(this);
		case SerializationFactory.JSON:
			return new JsonSerializationFactory(this);
		default:
			throw new SerializationException(format("Unknown serialization format '%s'", serializationFormat));
		}
	}

	@Override
	public TypeEvaluator typeEvaluator() {
		return defaultEvaluator;
	}
}
