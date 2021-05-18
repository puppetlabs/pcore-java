package com.puppet.pcore.impl;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.loader.BasicLoader;
import com.puppet.pcore.impl.loader.ParentedLoader;
import com.puppet.pcore.impl.loader.TypeSetLoader;
import com.puppet.pcore.impl.types.*;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;

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
	public final ObjectType target;
	public final ObjectType error;
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
		try {
			loader = new BasicLoader();
			implementationRegistry = new ImplementationRegistryImpl(null);
			typeEvaluator = new TypeEvaluatorImpl(this);

			data = typeEvaluator.declareType("Data", "Variant[ScalarData,Undef,Array[Data],Hash[String,Data]]");
			richDataKey = typeEvaluator.declareType("RichDataKey", "Variant[String,Numeric]");
			richData = typeEvaluator.declareType(
					"RichData",
					"Variant[Scalar,SemVerRange,Binary,Sensitive,Type,TypeSet,Default,Undef,Hash[RichDataKey,RichData],Array[RichData]]");

			data.resolve(this);
			richDataKey.resolve(this);
			richData.resolve(this);

			for(AnyType metaType : TypeFactory.registerPcoreTypes(this))
				metaType.resolve(this);
			TypeFactory.registerImpls(this);
			failWhenUnresolved = true;

  		target = (ObjectType)typeEvaluator.declareType("Target",
	  			"Object[\n" +
		  		"  attributes => {\n" +
			  	"    host => String[1],\n" +
				  "    options => { type => Hash[String[1], Data], value => {} }\n" +
				  "  }]");

			error = (ObjectType)typeEvaluator.declareType("Error",
					"Object[\n" +
					"  type_parameters => {\n" +
					"    kind => Optional[Variant[String,Regexp,Type[Enum],Type[Pattern],Type[NotUndef],Type[Undef]]],\n" +
					"    issue_code => Optional[Variant[String,Regexp,Type[Enum],Type[Pattern],Type[NotUndef],Type[Undef]]]\n" +
					"  },\n" +
					"  attributes => {\n" +
					"    message => String[1],\n" +
					"    kind => { type => Optional[String[1]], value => undef },\n" +
					"    issue_code => { type => Optional[String[1]], value => undef },\n" +
					"    partial_result => { type => Data, value => undef },\n" +
					"    details => { type => Optional[Hash[String[1],Data]], value => undef },\n" +
					"  }]");

			target.resolve(this);
			error.resolve(this);

			// Must request factoryDispatcher since it changes the implementation repository which is about to be frozen
			target.factoryDispatcher();
			error.factoryDispatcher();

		} catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
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
		target = staticPcoreInstance.target;
		error = staticPcoreInstance.error;
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
		return createObjectType(typeName, parentName, attributesHash, emptyMap(), emptyMap(), emptyList(), serialization);
	}

	public <C> ObjectType createObjectType(String typeName, String parentName, Map<String,Object>
			attributesHash, Map<String,Object> functionsHash, Map<String,Object>
			typeParametersHash, List<String> equality, List<String> serialization) {
		Map<String,Object> initHash = new HashMap<>();
		initHash.put(KEY_NAME, typeName);
		if(parentName != null)
			initHash.put(KEY_PARENT, typeReferenceType(parentName));
		if(!attributesHash.isEmpty())
			initHash.put(KEY_ATTRIBUTES, attributesHash);
		if(!functionsHash.isEmpty())
			initHash.put(KEY_FUNCTIONS, functionsHash);
		if(!typeParametersHash.isEmpty())
			initHash.put(KEY_TYPE_PARAMETERS, typeParametersHash);
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
	public TypeEvaluator typeEvaluator() {
		return typeEvaluator;
	}
}
