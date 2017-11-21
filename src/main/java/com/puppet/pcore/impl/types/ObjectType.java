package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.Constants;
import com.puppet.pcore.impl.DynamicObjectImpl;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.model.HashExpression;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.serialization.SerializationException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Constants.*;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

public class ObjectType extends MetaType implements PuppetObjectWithHash {

	public abstract class AnnotatedMember extends ModelObject implements Annotatable {
		public final String name;
		public final boolean override;
		public final AnyType type;
		private final boolean _final;

		private final Map<AnyType,Map<String,?>> annotations;

		AnnotatedMember(String name, Map<String,Object> initHash) {
			this.name = name;
			this.type = (AnyType)initHash.get(KEY_TYPE);
			this.override = getArgument(KEY_OVERRIDE, initHash, false);
			this._final = getArgument(KEY_FINAL, initHash, false);
			this.annotations = getArgument(KEY_ANNOTATIONS, initHash, emptyMap());
		}

		@Override
		public Map<AnyType,Map<String,?>> getAnnotations() {
			return annotations;
		}

		public int hash() {
			return name.hashCode() * 31 + type.hashCode();
		}

		public Map<String,Object> initHash() {
			Map<String,Object> result = new LinkedHashMap<>();
			Map<AnyType,Map<String,?>> annotations = getAnnotations();
			if(!annotations.isEmpty())
				result.put(Constants.KEY_ANNOTATIONS, annotations);
			result.put(KEY_TYPE, type);
			if(isFinal())
				result.put(KEY_FINAL, true);
			if(override)
				result.put(KEY_OVERRIDE, true);
			return result;
		}

		public boolean isFinal() {
			return _final;
		}

		public String label() {
			return format("%s %s[%s]", featureType(), ObjectType.this.label(), name);
		}

		@Override
		void accept(Visitor visitor, RecursionGuard guard) {
			for(AnyType key : getAnnotations().keySet())
				key.accept(visitor, guard);
			type.accept(visitor, guard);
		}

		void assertCanBeOverridden(AnnotatedMember member) {
			if(!getClass().equals(member.getClass()))
				throw new TypeResolverException(format("%s attempts to override %s", member.label(), label()));
			if(isFinal())
				throw new TypeResolverException(format("%s attempts to override final %s", member.label(), label()));
			if(!member.override)
				throw new TypeResolverException(format("%s attempts to override %s without having override => true", member
						.label(), label()));
			if(!type.isAssignable(member.type))
				throw new TypeResolverException(format("%s attempts to override %s with a type that does not match", member
						.label(), label()));
		}

		void assertOverride(Map<String,? extends AnnotatedMember> parentMembers) {
			AnnotatedMember pm = parentMembers.get(name);
			if(pm == null) {
				if(override)
					throw new TypeResolverException(format("expected %s to override inherited %s, but no such %s was found",
							label(), featureType(), featureType()));
			} else {
				pm.assertCanBeOverridden(this);
			}
		}

		@Override
		boolean guardedEquals(Object o, RecursionGuard guard) {
			if(getClass().equals(o.getClass())) {
				AnnotatedMember om = (AnnotatedMember)o;
				return override == om.override && _final == om._final && name.equals(om.name) && type.guardedEquals(om.type, guard);
			}
			return false;
		}

		abstract String featureType();
	}

	public class Attribute extends AnnotatedMember {
		public final AttributeKind kind;

		private final Object value;

		private Method getterMethod;

		Attribute(String name, Map<String,Object> initHash) {
			super(name, initHash);
			kind = initHash.containsKey(KEY_KIND)
					? AttributeKind.valueOf((String)initHash.get(KEY_KIND))
					: AttributeKind.normal;
			if(kind == AttributeKind.constant && Boolean.FALSE.equals(initHash.get(KEY_FINAL)))
				throw new TypeResolverException(format("%s of kind 'constant' cannot be combined with final => false", label
						()));

			if(initHash.containsKey(KEY_VALUE)) {
				switch(kind) {
				case derived:
				case given_or_derived:
					throw new TypeResolverException(format("%s of kind '%s' cannot be combined with an attribute value",
							label(), kind.name()));
				default:
					Object v = initHash.get(KEY_VALUE);
					if(!Default.SINGLETON.equals(v))
						type.assertInstanceOf(v, () -> String.format("%s %s", label(), KEY_VALUE));
					this.value = v;
				}
			} else {
				if(kind == AttributeKind.constant)
					throw new TypeResolverException(format("%s of kind  of kind 'constant' requires a value", label()));
				this.value = UNDEF;
			}
		}

		public boolean hasValue() {
			return value != UNDEF;
		}

		@Override
		public Map<String,Object> initHash() {
			return unmodifiableCopy(modifiableInitHash());
		}

		@Override
		boolean guardedEquals(Object o, RecursionGuard guard) {
			return super.guardedEquals(o, guard) && kind == ((Attribute)o).kind && Objects.equals(value, ((Attribute)o).value);
		}

		@Override
		public boolean isFinal() {
			return super.isFinal() || kind == AttributeKind.constant;
		}

		public Object value() {
			if(value == UNDEF)
				throw new TypeResolverException(format("%s has no value", label()));
			return value;
		}

		/** Return the value of the attribute in the given instance
		 *
		 * @param instance the instance to retrieve the attribute value from
		 * @return the attribute value
		 */
		public Object get(Object instance) {
			if(instance instanceof DynamicObject)
				return ((DynamicObject)instance).get(name);

			try {
				return getterMethod.invoke(instance);
			} catch(IllegalAccessException | InvocationTargetException e) {
				return null;
			}
		}

		private synchronized Method getterMethod(Class<?> implClass) throws NoSuchMethodException {
			if(getterMethod == null) {
				StringBuilder bld = new StringBuilder("get");
				bld.append(Character.toUpperCase(name.charAt(0)));
				bld.append(name.substring(1));
				getterMethod = implClass.getMethod(bld.toString());
			}
			return getterMethod;
		}

		@Override
		String featureType() {
			return "attribute";
		}

		Map<String,Object> modifiableInitHash() {
			Map<String,Object> result = super.initHash();
			if(kind != AttributeKind.normal) {
				result.put(KEY_KIND, kind.name());
				if(kind == AttributeKind.constant)
					result.remove(KEY_FINAL);
			}
			if(value != UNDEF)
				result.put(KEY_VALUE, value);
			return result;
		}
	}

	public class TypeParameter extends Attribute {
		TypeParameter(String name, Map<String,Object> initHash) {
			super(name, initHash);
		}

		@Override
		Map<String,Object> modifiableInitHash() {
			Map<String,Object> result = super.modifiableInitHash();
			AnyType type = (AnyType)result.get(KEY_TYPE);
			if(type instanceof OptionalType)
				type = ((OptionalType)type).type;
			result.put(KEY_TYPE, type);
			if(result.containsKey(KEY_VALUE) && result.get(KEY_VALUE) == null)
				result.remove(KEY_VALUE);
			return result;
		}

		@Override
		String featureType() {
			return "type_parameter";
		}
	}

	public class Function extends AnnotatedMember {
		Function(String name, Map<String,Object> initHash) {
			super(name, initHash);
		}

		@Override
		String featureType() {
			return "function";
		}

		@Override
		public Map<String,Object> initHash() {
			Map<String,Object> result = super.initHash();
			return unmodifiableCopy(result);
		}
	}

	private enum AttributeKind {constant, derived, given_or_derived, normal}

	private enum MemberType {attribute, function, all}

	static final ObjectType DEFAULT = new ObjectType();

	private static final AnyType TYPE_ATTRIBUTE_KIND = enumType(map(asList(AttributeKind.values()).subList(0, AttributeKind.values().length - 1), AttributeKind::name));
	private static final AnyType TYPE_MEMBER_NAME = patternType(regexpType(Pattern.compile("\\A[a-z_]\\w*\\z")));
	private static final AnyType TYPE_PARAMETER = variantType(typeType(), structType(
			structElement(KEY_TYPE, typeType()),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS)
	));
	private static final AnyType TYPE_ATTRIBUTE = variantType(typeType(), structType(
			structElement(KEY_TYPE, typeType()),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS),
			structElement(optionalType(KEY_FINAL), booleanType()),
			structElement(optionalType(KEY_OVERRIDE), booleanType()),
			structElement(optionalType(KEY_KIND), TYPE_ATTRIBUTE_KIND),
			structElement(optionalType(KEY_VALUE), anyType())
	));
	private static final AnyType TYPE_ATTRIBUTES = hashType(TYPE_MEMBER_NAME, notUndefType());
	private static final AnyType TYPE_PARAMETERS = hashType(TYPE_MEMBER_NAME, notUndefType());
	private static final AnyType TYPE_ATTRIBUTE_CALLABLE = callableType(tupleType(emptyList()));
	private static final AnyType TYPE_FUNCTION_TYPE = typeType(callableType());
	private static final AnyType TYPE_FUNCTION = variantType(TYPE_FUNCTION_TYPE, structType(
			structElement(KEY_TYPE, TYPE_FUNCTION_TYPE),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS),
			structElement(optionalType(KEY_FINAL), booleanType()),
			structElement(optionalType(KEY_OVERRIDE), booleanType())
	));
	private static final AnyType TYPE_MEMBER_NAMES = arrayType(TYPE_MEMBER_NAME);
	private static final AnyType TYPE_FUNCTIONS = hashType(TYPE_MEMBER_NAME, notUndefType());
	private static final AnyType TYPE_EQUALITY = variantType(TYPE_MEMBER_NAME, TYPE_MEMBER_NAMES);
	private static final AnyType TYPE_CHECKS = anyType(); // TBD
	static final StructType TYPE_OBJECT_INIT = structType(
			structElement(optionalType(KEY_NAME), TYPE_QUALIFIED_REFERENCE),
			structElement(optionalType(KEY_PARENT), typeType()),
			structElement(optionalType(KEY_TYPE_PARAMETERS), TYPE_PARAMETERS),
			structElement(optionalType(KEY_ATTRIBUTES), TYPE_ATTRIBUTES),
			structElement(optionalType(KEY_FUNCTIONS), TYPE_FUNCTIONS),
			structElement(optionalType(KEY_EQUALITY), TYPE_EQUALITY),
			structElement(optionalType(KEY_SERIALIZATION), TYPE_MEMBER_NAMES),
			structElement(optionalType(KEY_EQUALITY_INCLUDE_TYPE), booleanType()),
			structElement(optionalType(KEY_CHECKS), TYPE_CHECKS),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS)
	);
	private static final Object UNDEF = new Object();

	private static ObjectType ptype;
	private Map<String,TypeParameter> typeParameters = emptyMap();
	private Map<String,Attribute> attributes = emptyMap();
	private Object checks;
	private List<String> equality;
	private boolean equalityIncludeType = true;
	private Map<String,Function> functions = emptyMap();
	private StructType initType;
	private String name;
	private ParameterInfo parameterInfo;
	private AnyType parent;
	private List<String> serialization;

	private ObjectType() {
		super((Expression)null);
	}

	@SuppressWarnings("unchecked")
	ObjectType(ArgumentsAccessor args) throws IOException {
		super((Expression)null);
		args.remember(this);
		Map<String,Object> initHash = (Map<String,Object>)args.get(0);
		this.name = (String)TYPE_QUALIFIED_REFERENCE.assertInstanceOf(initHash.get(KEY_NAME), true, () -> "Object name");
		setInitHashExpression(initHash);
	}

	ObjectType(String name, Expression initHashExpression) {
		super(initHashExpression);
		this.name = TYPE_QUALIFIED_REFERENCE.assertInstanceOf(name, true, () -> "Object name");
	}

	public ObjectType(Map<String,Object> initHash) {
		super(initHash);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public Map<String,TypeParameter> typeParameters(boolean includeParent) {
		if(includeParent) {
			Map<String,TypeParameter> all = new LinkedHashMap<>();
			collectTypeParameters(all, includeParent);
			return all;
		}
		return typeParameters;
	}

	public Map<String,Attribute> attributes(boolean includeParent) {
		return members(includeParent, MemberType.attribute);
	}

	public Object[] attributeValuesFor(Object value) {
		Object[] args;
		if(value instanceof DynamicObjectImpl)
			args = ((DynamicObjectImpl)value).getAttributes();
		else {
			Class<?> implClass = value.getClass();
			java.util.function.Function<Object,Object[]> attributeProvider = pcore().implementationRegistry().attributeProviderFor(this);
			if(attributeProvider == null)
				throw new SerializationException(format("No attribute provider found for %s", implClass.getName()));

			args = attributeProvider.apply(value);
		}

		// Limit the array to not include trailing defaults
		int top = args.length;
		ParameterInfo pi = parameterInfo();
		while(--top >= 0) {
			Attribute attr = pi.attributes.get(top);
			if(!(attr.hasValue() && Objects.equals(attr.value(), args[top])))
				break;
		}
		++top;
		return top == args.length ? args : Arrays.copyOf(args, top);
	}

	public List<String> declaredEquality() {
		return equality;
	}

	public List<String> equality() {
		List<String> all = new ArrayList<>();
		collectEqualityAttributes(all);
		return all;
	}

	public Map<String,Function> functions(boolean includeParent) {
		return members(includeParent, MemberType.function);
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public Attribute getAttribute(String a) {
		Attribute attr = attributes.get(a);
		if(attr == null) {
			AnyType parent = resolveParent();
			if(parent instanceof ObjectType)
				attr = ((ObjectType)parent).getAttribute(a);
		}
		return attr;
	}

	public Function getFunction(String a) {
		Function attr = functions.get(a);
		if(attr == null) {
			AnyType parent = resolveParent();
			if(parent instanceof ObjectType)
				attr = ((ObjectType)parent).getFunction(a);
		}
		return attr;
	}

	public AnnotatedMember getMember(String a) {
		AnnotatedMember member = attributes.get(a);
		if(member == null) {
			member = functions.get(a);
			if(member == null) {
				AnyType parent = resolveParent();
				if(parent instanceof ObjectType)
					member = ((ObjectType)parent).getMember(a);
			}
		}
		return member;
	}

	public TypeParameter getTypeParameter(String a) {
		TypeParameter tp = typeParameters.get(a);
		if(tp == null) {
			AnyType parent = resolveParent();
			if(parent instanceof ObjectType)
				tp = ((ObjectType)parent).getTypeParameter(a);
		}
		return tp;
	}

	@Override
	@SuppressWarnings("unchecked")
	public FactoryDispatcher<?> factoryDispatcher() {
		ImplementationRegistry ir = pcore().implementationRegistry();
		FactoryDispatcher fd = ir.creatorFor(this);
		if(fd == null) {
			FactoryDispatcher<DynamicObjectImpl> dynFd = dispatcher(
					constructor(args -> new DynamicObjectImpl(this, args.toArray()), parameterInfo().parametersType()));
			ir.registerImplementation(this, dynFd, DynamicObjectImpl::getAttributes);
			fd = dynFd;
		}
		return fd;
	}

	public int hashCode() {
		if(name != null)
			return name.hashCode();
		return (Objects.hashCode(parent) * 31 + Objects.hashCode(attributes)) * 31 + Objects.hashCode(functions);
	}

	@Override
	public Map<String,Object> _pcoreInitHash() {
		return initHash(true);
	}

	public Map<String,Object> initHash(boolean includeName) {
		Map<String,Object> result = super._pcoreInitHash();
		if(includeName && name != null)
			result.put(KEY_NAME, name);
		if(parent != null)
			result.put(KEY_PARENT, parent);
		if(!typeParameters.isEmpty())
			result.put(KEY_TYPE_PARAMETERS, compressedMembersMap(typeParameters));
		if(!attributes.isEmpty())
			result.put(KEY_ATTRIBUTES, compressedMembersMap(attributes));
		if(!functions.isEmpty())
			result.put(KEY_FUNCTIONS, compressedMembersMap(functions));
		if(equality != null)
			result.put(KEY_EQUALITY, equality);
		if(serialization != null)
			result.put(KEY_SERIALIZATION, serialization);
		if(!equalityIncludeType)
			result.put(KEY_EQUALITY_INCLUDE_TYPE, false);
		return unmodifiableCopy(result);
	}

	public synchronized StructType initType() {
		if(initType == null)
			initType = createInitType();
		return initType;
	}

	public boolean isEqualityIncludeType() {
		return equalityIncludeType;
	}

	public boolean isParameterized() {
		if(typeParameters.isEmpty()) {
			AnyType parent = resolveParent();
		  return parent instanceof ObjectType && ((ObjectType)parent).isParameterized();
		}
		return true;
	}

	public String label() {
		return name == null ? "<anonymous object type>" : name;
	}

	@Override
	public String name() {
		return name == null ? "Object" : name;
	}

	@Override
	public Object newInstance(Object... args) {
		return factoryDispatcher().createInstance(this, args);
	}

	@Override
	public Object newInstance(ArgumentsAccessor aa) throws IOException {
		return aa.remember(factoryDispatcher().createInstance(this, aa));
	}

	@Override
	public synchronized ParameterInfo parameterInfo() {
		if(parameterInfo == null)
			parameterInfo = createParameterInfo();
		return parameterInfo;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::ObjectType", "Pcore::AnyType",
				asMap(
						KEY_NAME, TYPE_QUALIFIED_REFERENCE,
						KEY_PARENT, asMap(
								KEY_TYPE, optionalType(typeType()),
								KEY_VALUE, null),
						KEY_TYPE_PARAMETERS, asMap(
								KEY_TYPE, optionalType(TYPE_PARAMETERS),
								KEY_VALUE, null),
						KEY_ATTRIBUTES, asMap(
								KEY_TYPE, optionalType(TYPE_ATTRIBUTES),
								KEY_VALUE, null),
						KEY_FUNCTIONS, asMap(
								KEY_TYPE, optionalType(TYPE_FUNCTIONS),
								KEY_VALUE, null),
						KEY_EQUALITY, asMap(
								KEY_TYPE, optionalType(TYPE_EQUALITY),
								KEY_VALUE, null),
						KEY_SERIALIZATION, asMap(
								KEY_TYPE, optionalType(TYPE_MEMBER_NAMES),
								KEY_VALUE, null),
						KEY_EQUALITY_INCLUDE_TYPE, asMap(
								KEY_TYPE, optionalType(booleanType()),
								KEY_VALUE, null),
						KEY_CHECKS, asMap(
								KEY_TYPE, optionalType(TYPE_CHECKS),
								KEY_VALUE, null),
						KEY_ANNOTATIONS, asMap(
								KEY_TYPE, optionalType(TYPE_ANNOTATIONS),
								KEY_VALUE, null)
				));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, objectTypeDispatcher(),
				(self) -> new Object[]{self._pcoreInitHash()});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		guardedRecursion(guard, null, (g) -> {
			super.accept(visitor, g);
			if(parent != null)
				parent.accept(visitor, g);
			for(TypeParameter tp : typeParameters.values())
				tp.accept(visitor, g);
			for(Attribute a : attributes.values())
				a.accept(visitor, g);
			for(Function f : functions.values())
				f.accept(visitor, g);
			return null;
		});
	}

	@Override
	void checkSelfRecursion(AnyType originator) {
		if(parent != null) {
			if(parent == originator)
				throw new TypeResolverException(format("The Object type '%s' inherits from itself", label()));
			parent.checkSelfRecursion(originator);
		}
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o != null && getClass().equals(o.getClass())) {
			ObjectType to = (ObjectType)o;
			return Objects.equals(name, to.name)
					&& equals(parent, to.parent, guard)
					&& equals(typeParameters, to.typeParameters, guard)
					&& equals(attributes, to.attributes, guard)
					&& equals(functions, to.functions, guard)
					&& Objects.equals(equality, to.equality)
					&& Objects.equals(checks, to.checks);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	void initializeFromHash(Map<String,Object> initHash) {
		TYPE_OBJECT_INIT.assertInstanceOf(initHash, () -> "Object initializer");
		if(name == null)
			name = (String)initHash.get(KEY_NAME);
		parent = (AnyType)initHash.get(KEY_PARENT);

		Map<String,AnnotatedMember> pm = emptyMap();
		Map<String,TypeParameter> tp = emptyMap();
		ObjectType pot = null;
		if(parent != null) {
			checkSelfRecursion(this);
			AnyType rp = resolveParent();
			if(rp instanceof ObjectType) {
				pot = (ObjectType)rp;
				pm = pot.members(true, MemberType.all);
				tp = pot.typeParameters(true);
			}
		}

		ObjectType parentObjectType = pot;
		Map<String,AnnotatedMember> parentMembers = pm;

		Map<String,Object> typeParamSpecs = (Map<String,Object>)initHash.get(KEY_TYPE_PARAMETERS);
		if(!(typeParamSpecs == null || typeParamSpecs.isEmpty())) {
			Map<String,TypeParameter> typeParams = new LinkedHashMap<>();
			for(Entry<String,Object> p : typeParamSpecs.entrySet()) {
				Object typeParamSpec = TYPE_PARAMETER.assertInstanceOf(p.getValue(),
						() -> format("initializer for type_parameter %s[%s]", label(), p.getKey()));

				Object paramValue = null;
				AnyType paramType;
				if(!(typeParamSpec instanceof Map<?,?>))
					paramType = (AnyType)typeParamSpec;
				else {
					Map<String,Object> tm = (Map<String,Object>)typeParamSpec;
					paramType = (AnyType)tm.get(KEY_TYPE);
					if(tm.containsKey(KEY_VALUE))
						paramValue = tm.get(KEY_VALUE);
				}
				if(!paramType.isAssignable(UndefType.DEFAULT))
					paramType = new OptionalType(paramType);
				TypeParameter typeParam = new TypeParameter(p.getKey(), asMap(KEY_TYPE, paramType, KEY_VALUE, paramValue));
				typeParam.assertOverride(tp);
				typeParams.put(p.getKey(), typeParam);
			}
			typeParameters = unmodifiableMap(typeParams);
		}

		Map<String,Object> attrSpecs = (Map<String,Object>)initHash.get(KEY_ATTRIBUTES);
		if(!(attrSpecs == null || attrSpecs.isEmpty())) {
			Map<String,Attribute> attrs = new LinkedHashMap<>();
			for(Entry<String,Object> p : attrSpecs.entrySet()) {
				Object attrSpec = TYPE_ATTRIBUTE.assertInstanceOf(p.getValue(),
						() -> format("initializer for attribute %s[%s]", label(), p.getKey()));

				if(!(attrSpec instanceof Map<?,?>))
					attrSpec = (attrSpec instanceof OptionalType) ? asMap(KEY_TYPE, attrSpec, KEY_VALUE, null) : singletonMap(KEY_TYPE, attrSpec);

				Attribute attr = new Attribute(p.getKey(), (Map<String,Object>)attrSpec);
				attr.assertOverride(parentMembers);
				attrs.put(p.getKey(), attr);
			}
			attributes = unmodifiableMap(attrs);
		}

		Map<String,Object> funcSpecs = (Map<String,Object>)initHash.get(KEY_FUNCTIONS);
		if(!(funcSpecs == null || funcSpecs.isEmpty())) {
			Map<String,Function> funcs = new LinkedHashMap<>();
			for(Entry<String,Object> p : funcSpecs.entrySet()) {
				Object funcSpec = TYPE_FUNCTION.assertInstanceOf(p.getValue(),
						() -> format("initializer for function %s[%s]", label(), p.getKey()));

				if(!(funcSpec instanceof Map<?,?>))
					funcSpec = singletonMap(KEY_TYPE, funcSpec);

				Function func = new Function(p.getKey(), (Map<String,Object>)funcSpec);
				if(attributes.containsKey(func.name))
					throw new TypeResolverException(format("%s conflicts with attribute with the same name", func.label()));
				func.assertOverride(parentMembers);
				funcs.put(p.getKey(), func);
			}
			functions = unmodifiableMap(funcs);
		}

		equalityIncludeType = getArgument(KEY_EQUALITY_INCLUDE_TYPE, initHash, true);
		Object equality = initHash.get(KEY_EQUALITY);
		if(equality == null)
			this.equality = null;
		else {
			if(equality instanceof String)
				this.equality = singletonList((String)equality);
			else
				this.equality = unmodifiableCopy((List<String>)equality);

			if(!this.equality.isEmpty()) {
				if(!equalityIncludeType)
					throw new TypeResolverException(format(
							"%s equality_include_type = false cannot be combined with non empty equality specification",
							label()));

				List<String> parentEqAttrs = null;
				for(String attrName : this.equality) {
					AnnotatedMember attr = parentMembers.get(attrName);
					if(attr == null) {
						attr = attributes.get(attrName);
						if(attr == null)
							attr = functions.get(attrName);
					} else if(attr instanceof Attribute) {
						if(parentEqAttrs == null)
							parentEqAttrs = parentObjectType == null ? emptyList() : parentObjectType.equality();
						if(parentEqAttrs.contains(attrName)) {
							ObjectType includingParent = findEqualityDefiner(attrName);
							throw new TypeResolverException(format("%s equality is referencing %s which is included in equality of %s",
									label(), attr.label(), includingParent.label()));
						}
					}
					if(!(attr instanceof Attribute)) {
						if(attr == null)
							throw new TypeResolverException(format("%s equality is referencing non existent attribute '%s'",
									label(), attrName));
						throw new TypeResolverException(format("%s equality is referencing %s. Only attribute references are allowed",
								label(), attr.label()));
					}
					if(((Attribute)attr).kind == AttributeKind.constant)
						throw new TypeResolverException(format("%s equality is referencing constant %s. Reference to constant is not allowed in equality",
								label(), attr.label()));
				}
			}
		}
		serialization = (List<String>)initHash.get(KEY_SERIALIZATION);
		if(serialization != null) {
			Map<String,Attribute> attrs = attributes(true);
			Attribute optFound = null;
			for(String attrName : serialization) {
				Attribute attr = attrs.get(attrName);
				if(attr == null)
					throw new TypeResolverException(format("%s serialization is referencing non existent attribute '%s'",
							label(), attrName));
				if(attr.kind == AttributeKind.constant || attr.kind == AttributeKind.derived)
					throw new TypeResolverException(format("%s serialization is referencing %s %s. Reference to %s is not allowed in equality",
							label(), attr.kind, attr.label(), attr.kind));
				if(attr.hasValue())
					optFound = attr;
				else if(optFound != null)
					throw new TypeResolverException(
							format("%s serialization is referencing required %s after optional %s. Optional attributes must be last",
									label(), attr.label(), optFound.label()));
			}
		}
		checks = initHash.get(KEY_CHECKS);
		super.initializeFromHash(initHash);
	}

	private void collectEqualityAttributes(List<String> all) {
		AnyType parent = resolveParent();
		if(parent instanceof ObjectType)
			((ObjectType)parent).collectEqualityAttributes(all);
		if(equality == null) {
			// All attributes except constants participate
			for(Attribute attr : attributes.values())
				if(attr.kind != AttributeKind.constant)
					all.add(attr.name);
		} else
			all.addAll(equality);
	}

	private void collectMembers(Map<String,AnnotatedMember> all, boolean includeParent, MemberType memberType) {
		if(includeParent) {
			AnyType parent = resolveParent();
			if(parent instanceof ObjectType)
				((ObjectType)parent).collectMembers(all, true, memberType);
		}
		switch(memberType) {
		case attribute:
			all.putAll(attributes);
			break;
		case function:
			all.putAll(functions);
			break;
		default:
			all.putAll(attributes);
			all.putAll(functions);
		}
	}

	private void collectTypeParameters(Map<String,TypeParameter> all, boolean includeParent) {
		if(includeParent) {
			AnyType parent = resolveParent();
			if(parent instanceof ObjectType)
				((ObjectType)parent).collectTypeParameters(all, true);
		}
		all.putAll(typeParameters);
	}

	private Map<String,Object> compressedMembersMap(Map<String,? extends AnnotatedMember> members) {
		Map<String,Object> result = new LinkedHashMap<>();
		for(Entry<String,? extends AnnotatedMember> entry : members.entrySet()) {
			Map<String,Object> fh = entry.getValue().initHash();
			if(fh.size() == 1) {
				Object type = fh.get(KEY_TYPE);
				if(type != null) {
					result.put(entry.getKey(), type);
					continue;
				}
			}
			result.put(entry.getKey(), fh);
		}
		return result;
	}

	private StructType createInitType() {
		List<StructElement> elements = new ArrayList<>();
		for(Attribute attr : attributes(true).values()) {
			switch(attr.kind) {
			case constant:
			case derived:
				continue;
			default:
				if(attr.hasValue())
					elements.add(structElement(optionalType(attr.name), attr.type));
				else
					elements.add(structElement(attr.name, attr.type));
			}
		}
		return structType(elements);
	}

	private ParameterInfo createParameterInfo() {
		Map<String,Attribute> attributeMap = attributes(true);
		List<Attribute> attributes = new ArrayList<>();
		int nonOptSize;
		if(serialization == null) {
			List<Attribute> optAttributes = new ArrayList<>();
			for(Attribute attr : attributeMap.values()) {
				switch(attr.kind) {
				case constant:
				case derived:
					continue;
				default:
					if(attr.hasValue())
						optAttributes.add(attr);
					else
						attributes.add(attr);
				}
			}
			nonOptSize = attributes.size();
			attributes.addAll(optAttributes);
		} else {
			// Attribute order already checked.
			nonOptSize = 0;
			for(String key : serialization) {
				Attribute attr = attributeMap.get(key);
				if(!attr.hasValue())
					++nonOptSize;
				attributes.add(attr);
			}
		}
		return new ParameterInfo(attributes, nonOptSize, equality());
	}

	private ObjectType findEqualityDefiner(String attrName) {
		ObjectType type = this;
		while(true) {
			AnyType p = type.resolveParent();
			if(!(p instanceof ObjectType))
				return type;
			if(!((ObjectType)p).equality().contains(attrName))
				return type;
			type = (ObjectType)p;
		}
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof PuppetObject && isAssignable((AnyType)((PuppetObject)o)._pcoreType(), guard);

	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof ObjectTypeExtension)
			t = ((ObjectTypeExtension)t).baseType;

		if(t instanceof ObjectType) {
			ObjectType ot = (ObjectType)t;
			if(DEFAULT.equals(this) || equals(ot))
				return true;
			AnyType parent = ot.parent;
			return parent != null && isUnsafeAssignable(parent, guard);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T extends AnnotatedMember> Map<String,T> members(boolean includeParent, MemberType memberType) {
		if(includeParent || memberType == MemberType.all) {
			Map<String,AnnotatedMember> all = new LinkedHashMap<>();
			collectMembers(all, includeParent, memberType);
			return (Map<String,T>)all;
		}
		return (Map<String,T>)(memberType == MemberType.attribute ? attributes : functions);
	}

	@SuppressWarnings("unchecked")
	Map<String,Object> resolveLiteralHash(HashExpression i12e) {
		return resolveHash(super.resolveLiteralHash(i12e));
	}

	private AnyType resolveParent() {
		AnyType p = parent;
		while(p instanceof TypeAliasType)
			p = p.resolvedType();
		return p;
	}
}
