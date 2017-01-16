package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.DynamicObjectImpl;
import com.puppet.pcore.impl.GivenArgumentsAccessor;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.FactoryFunction;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Constants.*;
import static com.puppet.pcore.impl.Helpers.getArgument;
import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.*;

public class ObjectType extends MetaType {
	public static class ParameterInfo {
		public final Map<String,Integer> attributeIndex;
		public final List<Attribute> attributes;
		public final int[] equalityAttributeIndexes;
		public final int requiredCount;

		ParameterInfo(List<Attribute> attributes, int requiredCount, List<String> equality) {
			Map<String,Integer> attributeIndex = new HashMap<>();
			int idx = attributes.size();
			while(--idx >= 0)
				attributeIndex.put(attributes.get(idx).name, idx);

			this.attributes = Collections.unmodifiableList(attributes);
			this.attributeIndex = Collections.unmodifiableMap(attributeIndex);
			this.requiredCount = requiredCount;

			int top = equality.size();
			int[] ei = new int[top];
			for(idx = 0; idx < top; ++idx)
				ei[idx] = attributeIndex.get(equality.get(idx));
			this.equalityAttributeIndexes = ei;
		}
	}

	public abstract class AnnotatedMember extends ModelObject implements Annotatable {
		public final String name;
		public final boolean override;
		public final AnyType type;
		private final boolean _final;

		private final Map<AnyType,Map<String,?>> annotations;

		AnnotatedMember(String name, Map<String,Object> i12nHash) {
			this.name = name;
			this.type = (AnyType)i12nHash.get(KEY_TYPE);
			this.override = getArgument(KEY_OVERRIDE, i12nHash, false);
			this._final = getArgument(KEY_FINAL, i12nHash, false);
			this.annotations = getArgument(KEY_ANNOTATIONS, i12nHash, emptyMap());
		}

		public boolean equals(Object o) {
			if(getClass().equals(o.getClass())) {
				AnnotatedMember om = (AnnotatedMember)o;
				return override == om.override && _final == om._final && name.equals(om.name) && type.equals(om.type);
			}
			return false;
		}

		@Override
		public Map<AnyType,Map<String,?>> getAnnotations() {
			return annotations;
		}

		public int hash() {
			return name.hashCode() * 31 + type.hashCode();
		}

		@Override
		public Map<String,Object> i12nHash() {
			Map<String,Object> result = Annotatable.super.i12nHash();
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
			annotatableAccept(visitor, guard);
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

		abstract String featureType();
	}

	public class Attribute extends AnnotatedMember {
		public final AttributeKind kind;

		private final Object value;

		Attribute(String name, Map<String,Object> i12nHash) {
			super(name, i12nHash);
			kind = i12nHash.containsKey(KEY_KIND)
					? AttributeKind.valueOf((String)i12nHash.get(KEY_KIND))
					: AttributeKind.normal;
			if(kind == AttributeKind.constant && Boolean.FALSE.equals(i12nHash.get(KEY_FINAL)))
				throw new TypeResolverException(format("%s of kind 'constant' cannot be combined with final => false", label
						()));

			if(i12nHash.containsKey(KEY_VALUE)) {
				switch(kind) {
				case derived:
				case given_or_derived:
					throw new TypeResolverException(format("%s of kind '%s' cannot be combined with an attribute value",
							label(), kind.name()));
				default:
					Object v = i12nHash.get(KEY_VALUE);
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
		public Map<String,Object> i12nHash() {
			Map<String,Object> result = super.i12nHash();
			if(kind != AttributeKind.normal) {
				result.put(KEY_KIND, kind.name());
				if(kind == AttributeKind.constant)
					result.remove(KEY_FINAL);
			}
			if(value != UNDEF)
				result.put(KEY_VALUE, value);
			return result;
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

		@Override
		String featureType() {
			return "attribute";
		}
	}

	public class Function extends AnnotatedMember {
		Function(String name, Map<String,Object> i12nHash) {
			super(name, i12nHash);
		}

		@Override
		String featureType() {
			return "function";
		}
	}

	private enum AttributeKind {constant, derived, given_or_derived, normal}

	private enum MemberType {attribute, function, all}

	public static final ObjectType DEFAULT = new ObjectType();
	private static final AnyType TYPE_ATTRIBUTE_KIND = enumType(Arrays.copyOf(AttributeKind.values(), AttributeKind
			.values().length - 1));
	private static final AnyType TYPE_MEMBER_NAME = patternType(regexpType(Pattern.compile("\\A[a-z_]\\w*\\z")));
	private static final AnyType TYPE_ATTRIBUTE = variantType(typeType(), structType(
			structElement(KEY_TYPE, typeType()),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS),
			structElement(optionalType(KEY_FINAL), booleanType()),
			structElement(optionalType(KEY_OVERRIDE), booleanType()),
			structElement(optionalType(KEY_KIND), TYPE_ATTRIBUTE_KIND),
			structElement(optionalType(KEY_VALUE), anyType())
	));
	private static final AnyType TYPE_ATTRIBUTES = hashType(TYPE_MEMBER_NAME, notUndefType());
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
	private static final AnyType TYPE_OBJECT_I12N = structType(
			structElement(optionalType(KEY_NAME), TYPE_QUALIFIED_REFERENCE),
			structElement(optionalType(KEY_PARENT), typeType()),
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
	private Map<String,Attribute> attributes = emptyMap();
	private Object checks;
	private List<String> equality;
	private boolean equalityIncludeType = true;
	private Map<String,Function> functions = emptyMap();
	private StructType i12nType;
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
		Map<String,Object> i12nHash = (Map<String,Object>)args.get(0);
		this.name = (String)TYPE_QUALIFIED_REFERENCE.assertInstanceOf(i12nHash.get(KEY_NAME), true, () -> "Object name");
		setI12nHashExpression(i12nHash);
	}

	ObjectType(String name, Expression i12nHashExpression) {
		super(i12nHashExpression);
		this.name = TYPE_QUALIFIED_REFERENCE.assertInstanceOf(name, true, () -> "Object name");
	}

	ObjectType(String name, Map<String,Object> unresolvedI12nHash) {
		super(unresolvedI12nHash);
		this.name = TYPE_QUALIFIED_REFERENCE.assertInstanceOf(name, true, () -> "Object name");
	}

	ObjectType(Map<String,Object> i12nHash) {
		super(i12nHash);
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public Map<String,Attribute> attributes(boolean includeParent) {
		return members(includeParent, MemberType.attribute);
	}

	public List<String> declaredEquality() {
		return equality;
	}

	public List<String> equality() {
		List<String> all = new ArrayList<>();
		collectEqualityAttributes(all);
		return all;
	}

	public boolean equals(Object o) {
		if(getClass().equals(o.getClass())) {
			ObjectType to = (ObjectType)o;
			return Objects.equals(name, to.name)
					&& Objects.equals(parent, to.parent)
					&& attributes.equals(to.attributes)
					&& functions.equals(to.functions)
					&& Objects.equals(equality, to.equality)
					&& Objects.equals(checks, to.checks);
		}
		return false;
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

	public int hashCode() {
		if(name != null)
			return name.hashCode();
		return (Objects.hashCode(parent) * 31 + Objects.hashCode(attributes)) * 31 + Objects.hashCode(functions);
	}

	@Override
	public Map<String,Object> i12nHash() {
		return i12nHash(true);
	}

	public Map<String,Object> i12nHash(boolean includeName) {
		Map<String,Object> result = super.i12nHash();
		if(includeName && name != null)
			result.put(KEY_NAME, name);
		if(parent != null)
			result.put(KEY_PARENT, parent);
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
		return result;
	}

	public synchronized StructType i12nType() {
		if(i12nType == null)
			i12nType = createI12nType();
		return i12nType;
	}

	public boolean isEqualityIncludeType() {
		return equalityIncludeType;
	}

	public String label() {
		return name == null ? "<anonymous object type>" : name;
	}

	@Override
	public String name() {
		return name;
	}

	public Object newInstance(Pcore pcore, ArgumentsAccessor aa) throws IOException {
		ImplementationRegistry ir = pcore.implementationRegistry();
		Class<?> implClass = ir.classFor(this, currentThread().getContextClassLoader());
		FactoryFunction creator = implClass == null ? null : ir.creatorFor(implClass);
		return creator == null ? new DynamicObjectImpl(aa) : aa.remember(creator.createInstance(aa));
	}

	public Object newInstance(Pcore pcore, Object... args) {
		try {
			return newInstance(pcore, new GivenArgumentsAccessor(args, this));
		} catch(IOException e) {
			throw new PCoreException(e);
		}
	}

	public synchronized ParameterInfo parameterInfo() {
		if(parameterInfo == null)
			parameterInfo = createParameterInfo();
		return parameterInfo;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(ObjectType.class, "Pcore::ObjectType", "Pcore::AnyType",
				singletonMap("i12nHash", TYPE_OBJECT_I12N),
				ObjectType::new,
				(self) -> new Object[]{self.i12nHash()});
	}

	@Override
	void checkSelfRecursion(AnyType originator) {
		if(parent != null) {
			if(parent == originator)
				throw new TypeResolverException(format("The Object type '%s' inherits from itself", label()));
			parent.checkSelfRecursion(originator);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void initializeFromHash(Map<String,Object> i12nHash) {
		TYPE_OBJECT_I12N.assertInstanceOf(i12nHash, () -> "Object initializer");
		if(name == null)
			name = (String)i12nHash.get(KEY_NAME);
		parent = (AnyType)i12nHash.get(KEY_PARENT);

		Map<String,AnnotatedMember> pm = emptyMap();
		ObjectType pot = null;
		if(parent != null) {
			checkSelfRecursion(this);
			AnyType rp = resolveParent();
			if(rp instanceof ObjectType) {
				pot = (ObjectType)rp;
				pm = pot.members(true, MemberType.all);
			}
		}

		ObjectType parentObjectType = pot;
		Map<String,AnnotatedMember> parentMembers = pm;

		Map<String,Object> attrSpecs = (Map<String,Object>)i12nHash.get(KEY_ATTRIBUTES);
		if(!(attrSpecs == null || attrSpecs.isEmpty())) {
			Map<String,Attribute> attrs = new LinkedHashMap<>();
			for(Entry<String,Object> p : attrSpecs.entrySet()) {
				Object attrSpec = TYPE_ATTRIBUTE.assertInstanceOf(p.getValue(),
						() -> format("initializer for attribute %s[%s]", label(), p.getKey()));

				if(!(attrSpec instanceof Map<?,?>))
					attrSpec = singletonMap(KEY_TYPE, attrSpec);

				Attribute attr = new Attribute(p.getKey(), (Map<String,Object>)attrSpec);
				attr.assertOverride(parentMembers);
				attrs.put(p.getKey(), attr);
			}
			attributes = unmodifiableMap(attrs);
		}

		Map<String,Object> funcSpecs = (Map<String,Object>)i12nHash.get(KEY_FUNCTIONS);
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

		equalityIncludeType = getArgument(KEY_EQUALITY_INCLUDE_TYPE, i12nHash, true);
		Object equality = i12nHash.get(KEY_EQUALITY);
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
		serialization = (List<String>)i12nHash.get(KEY_SERIALIZATION);
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
		checks = i12nHash.get(KEY_CHECKS);
		super.initializeFromHash(i12nHash);
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
				((ObjectType)parent).collectMembers(all, includeParent, memberType);
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

	private Map<String,Object> compressedMembersMap(Map<String,? extends AnnotatedMember> members) {
		Map<String,Object> result = new LinkedHashMap<>();
		for(Entry<String,? extends AnnotatedMember> entry : members.entrySet()) {
			Map<String,Object> fh = entry.getValue().i12nHash();
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

	private StructType createI12nType() {
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

	@SuppressWarnings("unchecked")
	private <T extends AnnotatedMember> Map<String,T> members(boolean includeParent, MemberType memberType) {
		if(includeParent || memberType == MemberType.all) {
			Map<String,AnnotatedMember> all = new LinkedHashMap<>();
			collectMembers(all, includeParent, memberType);
			return (Map<String,T>)all;
		}
		return (Map<String,T>)(memberType == MemberType.attribute ? attributes : functions);
	}

	private AnyType resolveParent() {
		AnyType p = parent;
		while(p instanceof TypeAliasType)
			p = p.resolvedType();
		return p;
	}
}
