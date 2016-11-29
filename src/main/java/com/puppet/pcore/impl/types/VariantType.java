package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.*;
import java.util.stream.Stream;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class VariantType extends TypesContainerType {
	public static final VariantType DEFAULT = new VariantType(Collections.emptyList());
	public static final AnyType DATA = new VariantType(asList(scalarDataType(), undefType(), ArrayType.DATA, HashType
			.DATA));

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
	public Type _pType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return this.equals(DEFAULT)
				? DEFAULT
				: variantType(types.stream().map(AnyType::generalize).distinct());
	}

	@SuppressWarnings("unchecked")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(VariantType.class, "Pcore::VariantType", "Pcore::AnyType",
				asMap("types", arrayType(typeType())),
				(args) -> new VariantType((List<AnyType>)args.get(0)),
				(self) -> new Object[]{self.types});
	}

	@Override
	TypesContainerType copyWith(List<AnyType> types, boolean resolved) {
		return new VariantType(types, resolved);
	}

	@Override
	int isReallyInstance(Object o, RecursionGuard guard) {
		return types.stream().mapToInt(t -> t.isReallyInstance(o, guard)).reduce(-1, (memo, r) -> r > memo ? r : memo);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof VariantType) {
			//  A variant is assignable if all of its options are assignable to one of this type's options
			return this == type || ((VariantType)type).types.stream().allMatch(other -> other instanceof DataType
					? isAssignable(DATA, guard)
					: (other instanceof VariantType
							? isAssignable(other, guard)
							: types.stream().anyMatch(variant -> variant.isAssignable(other, guard))));
		}
		return types.stream().anyMatch(variant -> variant.isAssignable(type, guard));
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return variantType(Helpers.mergeUnique(types, ((VariantType)other).types));
	}

	private static Stream<AnyType> mergeEnums(List<AnyType> enums) {
		return enums.size() < 2
				? enums.stream()
				: Stream.of(enumType(enums.stream().flatMap(eos ->
						eos instanceof StringType
								? Stream.of(((StringType)eos).value)
								: ((EnumType)eos).enums.stream()).distinct()));
	}

	private static Stream<PatternType> mergePatterns(List<PatternType> patterns) {
		return patterns.size() < 2
				? patterns.stream()
				: Stream.of(patternType(patterns.stream().flatMap(pattern -> pattern.regexps.stream()).distinct()));
	}

	@SuppressWarnings("unchecked")
	private static List<AnyType> partitionAndMerge(List<? extends AnyType> types) {
		Map<Class<? extends AnyType>,? extends List<? extends AnyType>> pm = types.stream().collect(
				groupingBy(AnyType::getClass, LinkedHashMap::new, toList()));

		ArrayList<AnyType> enumsAndStrings = new ArrayList<>(remove(StringType.class, pm));
		enumsAndStrings.addAll(remove(EnumType.class, pm));

		return Stream.of(
				mergeEnums(enumsAndStrings),
				mergePatterns(remove(PatternType.class, pm)),
				Helpers.mergeRanges(remove(IntegerType.class, pm)),
				Helpers.mergeRanges(remove(FloatType.class, pm)),
				Helpers.mergeRanges(remove(TimeSpanType.class, pm)),
				Helpers.mergeRanges(remove(TimestampType.class, pm)),
				pm.values().stream().flatMap(Collection::stream)
		).flatMap(x -> x).collect(toList());
	}

	@SuppressWarnings("unchecked")
	private static <T extends AnyType> List<T> remove(
			Class<T> cls, Map<Class<? extends AnyType>,? extends List<?
			extends AnyType>> pm) {
		List<? extends AnyType> removed = pm.remove(cls);
		return removed == null ? Collections.emptyList() : (List<T>)removed;
	}
}
