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

import static com.puppet.pcore.impl.Constants.*;
import static com.puppet.pcore.impl.types.TypeFactory.objectType;
import static com.puppet.pcore.impl.types.TypeFactory.typeReferenceType;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class PcoreImpl extends Pcore {
	public final ImplementationRegistryImpl implementationRegistry;
	public final Loader loader;
	public final TypeEvaluatorImpl typeEvaluator;
	public final AnyType data;
	public final AnyType richDataKey;
	public final AnyType richData;
	public final boolean failWhenUnresolved;

	private static PcoreImpl staticPcoreInstance = null;

	static {
		new PcoreImpl();
	}

	public static PcoreImpl staticInstance() {
		return staticPcoreInstance;
	}

	// Private constructor. Only used when staticPcore is initialized
	private PcoreImpl() {
		staticPcoreInstance = this;
		loader = new BasicLoader();
		implementationRegistry = new ImplementationRegistryImpl(null);
		typeEvaluator = new TypeEvaluatorImpl(this);

		data = typeEvaluator.declareType("Data", "Variant[ScalarData,Undef,Array[Data],Hash[String,Data]]");
		richDataKey = typeEvaluator.declareType("RichDataKey", "Variant[String,Numeric]");
		richData = typeEvaluator.declareType("RichData", "Variant[Scalar,SemVerRange,Binary,Sensitive,Type,TypeSet,Default,Undef,Hash[RichDataKey,RichData],Array[RichData]]");

		data.resolve(this);
		richDataKey.resolve(this);
		richData.resolve(this);

		for(AnyType metaType : TypeFactory.registerPcoreTypes(this))
			metaType.resolve(this);
		TypeFactory.registerImpls(this);
		failWhenUnresolved = true;
		freeze(); // Static Pcore is always frozen
	}

	public PcoreImpl(Loader loader, boolean failWhenUnresolved) {
		this.loader = loader;
		this.failWhenUnresolved = failWhenUnresolved;

		implementationRegistry = new ImplementationRegistryImpl(staticPcoreInstance.implementationRegistry);
		typeEvaluator = new TypeEvaluatorImpl(this);
		data = staticPcoreInstance.data;
		richDataKey = staticPcoreInstance.richDataKey;
		richData = staticPcoreInstance.richData;
	}

	public ObjectType createObjectType(
			String typeName, String parentName) {
		return createObjectType(typeName, parentName, emptyMap());
	}

	@Override
	public boolean failWhenUnresolved() {
		return failWhenUnresolved;
	}

	@Override
	public void freeze() {
		implementationRegistry.freeze(); // Static Pcore is always frozen
		loader.freeze();
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

	@Override
	public Loader loader() {
		return loader;
	}

	@Override
	public ImplementationRegistry implementationRegistry() {
		return implementationRegistry;
	}

	@Override
	public Pcore withLocalScope() {
		return new PcoreImpl(new ParentedLoader(loader), failWhenUnresolved);
	}

	/**
	 * Create a Pcore instance that uses a loader that is parented by the current loader and capable of
	 * finding things in the given type set.
	 *
	 * @param typeSet the type set to add on top of the current scope
	 * @return the new Pcore instance
	 */
	@Override
	public Pcore withTypeSetScope(TypeSetType typeSet) {
		return new PcoreImpl(new TypeSetLoader(loader, typeSet), failWhenUnresolved);
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
			return new MsgPackSerializationFactory();
		case SerializationFactory.JSON:
			return new JsonSerializationFactory();
		default:
			throw new SerializationException(format("Unknown serialization format '%s'", serializationFormat));
		}
	}

	@Override
	public TypeEvaluator typeEvaluator() {
		return typeEvaluator;
	}
}
