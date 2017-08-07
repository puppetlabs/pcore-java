package com.puppet.pcore.impl;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.loader.BasicLoader;
import com.puppet.pcore.impl.loader.ParentedLoader;
import com.puppet.pcore.impl.loader.TypeSetLoader;
import com.puppet.pcore.impl.types.*;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.serialization.SerializationException;
import com.puppet.pcore.impl.serialization.json.JsonSerializationFactory;
import com.puppet.pcore.impl.serialization.msgpack.MsgPackSerializationFactory;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;
import com.puppet.pcore.serialization.SerializationFactory;

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
	private ImplementationRegistry implementationRegistry;
	private final ThreadLocal<Loader> loader = new ThreadLocal<>();
	private TypeEvaluatorImpl typeEvaluator;

	public void initBaseTypeSystem() {
		implementationRegistry = new ImplementationRegistryImpl();
		loader.set(new BasicLoader());
		typeEvaluator = new TypeEvaluatorImpl();
		typeEvaluator.resolveAliases();
		for(AnyType metaType : TypeFactory.registerPcoreTypes(this))
			metaType.resolve();
		TypeFactory.registerImpls(this);
	}

	public <C> ObjectType createObjectType(
			String typeName, String parentName) {
		return createObjectType(typeName, parentName, emptyMap());
	}

	public <T> void registerImpl(ObjectType type, FactoryDispatcher<T> creator, Function<T,Object[]> attributeSupplier) {
		implementationRegistry.registerImplementation(type, creator, attributeSupplier);
	}

	public <T> void registerImpl(ObjectType type, FactoryDispatcher<T> creator) {
		registerImpl(type, creator, (self) -> EMPTY_ARRAY);
	}

	public <C> ObjectType createObjectType(
			String typeName, String parentName, Map<String,Object>
			attributesHash) {
		return createObjectType(typeName, parentName, attributesHash, emptyList());
	}

	public <C> ObjectType createObjectType(
			String typeName, String parentName, Map<String,Object>
			attributesHash, List<String> serialization) {
		return createObjectType(typeName, parentName, attributesHash, emptyMap(), emptyList(), serialization);
	}

	public <C> ObjectType createObjectType(String typeName, String parentName, Map<String,Object>
			attributesHash, Map<String,Object> functionsHash, List<String> equality, List<String> serialization) {
		Map<String,Object> initHash = new HashMap<>();
		initHash.put(KEY_NAME, typeName);
		if(parentName != null)
			initHash.put(KEY_PARENT, typeReferenceType(parentName));
		if(!attributesHash.isEmpty())
			initHash.put(KEY_ATTRIBUTES, attributesHash);
		if(!functionsHash.isEmpty())
			initHash.put(KEY_FUNCTIONS, functionsHash);
		if(!equality.isEmpty())
			initHash.put(KEY_EQUALITY, equality);
		if(!serialization.isEmpty())
			initHash.put(KEY_SERIALIZATION, serialization);
		ObjectType type = objectType(initHash);
		loader().bind(new TypedName("type", typeName), type);
		return type;
	}

	public ImplementationRegistry implementationRegistry() {
		return implementationRegistry;
	}

	public <T> T withLocalScope(Supplier<T> function) {
		return withLoader(new ParentedLoader(loader()), function);
	}

	/**
	 * Execute function using a loader that is parented by the current loader and capable of finding things
	 * in the given type set.
	 *
	 * @param typeSet the type set to add on top of the current scope
	 * @param function the function to execute with the new scope
	 * @param <T> the return type
	 * @return the return value of the given function
	 */
	public <T> T withTypeSetScope(TypeSetType typeSet, Supplier<T> function) {
		return withLoader(new TypeSetLoader(loader(), typeSet), function);
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
		if(typeEvaluator == null)
			throw new IllegalStateException("base type system is not yet initialized");
		return typeEvaluator;
	}

	private <T> T withLoader(Loader localLoader, Supplier<T> function) {
		Loader current = loader.get();
		loader.set(localLoader);
		try {
			return function.get();
		} finally {
			loader.set(current);
		}
	}
}
