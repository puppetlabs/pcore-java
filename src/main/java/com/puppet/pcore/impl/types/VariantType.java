package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.*;

import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class VariantType extends TypesContainerType {
	static final VariantType DEFAULT = new VariantType(Collections.emptyList());

	private static ObjectType ptype;

	VariantType(List<AnyType> variants) {
		this(variants, false);
	}

	private VariantType(List<AnyType> variants, boolean resolved) {
		super(variants, resolved);
	}

	public static List<AnyType> normalize(List<? extends AnyType> variants) {
		return partitionAndMerge(variants);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return this.equals(DEFAULT)
				? DEFAULT
				: variantType(distinct(map(types, AnyType::generalize)));
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::VariantType", "Pcore::AnyType",
				asMap("types", arrayType(typeType())));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, variantTypeDispatcher(),
				(self) -> new Object[]{self.types});
	}

	@Override
	TypesContainerType copyWith(List<AnyType> types, boolean resolved) {
		return new VariantType(types, resolved);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		for(AnyType type : types)
			if(type.isInstance(o, guard))
				return true;
		return false;
	}

	@Override
	int isReallyInstance(Object o, RecursionGuard guard) {
		int state = -1;
		for(AnyType type : types) {
			int r = type.isReallyInstance(o, guard);
			if(r == 1)
				return 1;
			if(r > state)
				state = r;
		}
		return state;
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof VariantType) {
			//  A variant is assignable if any of its options are assignable to one of this type's options
			return this == type || all(((VariantType)type).types, other -> other instanceof VariantType
							? isAssignable(other, guard)
							: any(types, variant -> variant.isAssignable(other, guard)));
		}
		return any(types, variant -> variant.isAssignable(type, guard));
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return variantType(Helpers.mergeUnique(types, ((VariantType)other).types));
	}

	private static List<AnyType> mergeEnums(List<AnyType> enums) {
		return enums.size() < 2
				? enums
				: singletonList(enumType(distinct(flatten(map(enums, eos -> eos instanceof StringType ? ((StringType)eos).value : ((EnumType)eos).enums)))));
	}

	private static List<PatternType> mergePatterns(List<PatternType> patterns) {
		return patterns.size() < 2
				? patterns
				: singletonList(patternType(distinct(flatten(map(patterns, pattern -> pattern.regexps)))));
	}

	@SuppressWarnings("unchecked")
	private static List<AnyType> partitionAndMerge(List<? extends AnyType> types) {
		Map<Class<? extends AnyType>,? extends List<? extends AnyType>> pm = new LinkedHashMap<>(groupBy(types, AnyType::getClass));

		ArrayList<AnyType> enumsAndStrings = new ArrayList<>(remove(StringType.class, pm));
		enumsAndStrings.addAll(remove(EnumType.class, pm));

		return flatten(asList(
				mergeEnums(enumsAndStrings),
				mergePatterns(remove(PatternType.class, pm)),
				mergeRanges(remove(IntegerType.class, pm)),
				mergeRanges(remove(FloatType.class, pm)),
				mergeRanges(remove(TimeSpanType.class, pm)),
				mergeRanges(remove(TimestampType.class, pm)),
				pm.values()
		));
	}

	@SuppressWarnings("unchecked")
	private static <T extends AnyType> List<T> remove(
			Class<T> cls, Map<Class<? extends AnyType>,? extends List<?
			extends AnyType>> pm) {
		List<? extends AnyType> removed = pm.remove(cls);
		return removed == null ? Collections.emptyList() : (List<T>)removed;
	}
}
