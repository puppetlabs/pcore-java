package com.puppet.pcore.impl;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.loader.BasicLoader;
import com.puppet.pcore.impl.loader.TypeSetLoader;
import com.puppet.pcore.impl.serialization.SerializationException;
import com.puppet.pcore.impl.serialization.json.JsonSerializationFactory;
import com.puppet.pcore.impl.serialization.msgpack.MsgPackSerializationFactory;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.impl.types.TypeFactory;
import com.puppet.pcore.impl.types.TypeSetType;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;
import com.puppet.pcore.serialization.FactoryFunction;
import com.puppet.pcore.serialization.SerializationFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.puppet.pcore.impl.Constants.*;
import static com.puppet.pcore.impl.types.TypeFactory.objectType;
import static com.puppet.pcore.impl.types.TypeFactory.typeReferenceType;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class PcoreImpl {
	private final ImplementationRegistry implementationRegistry = new ImplementationRegistryImpl();
	private final TypeEvaluator typeEvaluator = new TypeEvaluatorImpl();
	private final ThreadLocal<Loader> loader = new ThreadLocal<Loader>();

	public void initBaseTypeSystem() {
		loader.set(new BasicLoader());
		try {
			Collection<AnyType> basicTypes = TypeEvaluatorImpl.BASIC_TYPES.values();
			List<ObjectType> metaTypes = new ArrayList<>(basicTypes.size());
			for(AnyType type : basicTypes) {
				Method registerPtypeMethod = type.getClass().getDeclaredMethod("registerPcoreType", PcoreImpl.class);
				registerPtypeMethod.setAccessible(true);
				metaTypes.add((ObjectType)registerPtypeMethod.invoke(null, this));
			}
			for(ObjectType metaType : metaTypes)
				metaType.resolve(typeEvaluator);
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
		loader().bind(new TypedName("type", typeName), type);
		return type;
	}

	public ImplementationRegistry implementationRegistry() {
		return implementationRegistry;
	}

	/**
	 * Execute function using a loader that is parented by the current loader and capable of finding things
	 * in the given type set.
	 *
	 * @param typeSet
	 * @param function
	 * @param <T>
	 * @return the return value of the given function
	 */
	public <T> T withTypeSetLoader(TypeSetType typeSet, Supplier<T> function) {
		Loader current = loader.get();
		loader.set(new TypeSetLoader(current, typeSet));
		try {
			return function.get();
		} finally {
			loader.set(current);
		}
	}

	public Loader loader() {
		return loader.get();
	}

	public Type infer(Object value) {
		return TypeFactory.infer(value);
	}

	public Type inferSet(Object value) {
		return TypeFactory.inferSet(value);
	}

	public SerializationFactory serializationFactory(String serializationFormat) {
		switch(serializationFormat) {
		case SerializationFactory.MSGPACK:
			return new MsgPackSerializationFactory();
		case SerializationFactory.JSON:
			return new JsonSerializationFactory();
		default:
			throw new SerializationException(format("Unknown serialization format '%s'", serializationFormat));
		}
	}

	public TypeEvaluator typeEvaluator() {
		return typeEvaluator;
	}
}
