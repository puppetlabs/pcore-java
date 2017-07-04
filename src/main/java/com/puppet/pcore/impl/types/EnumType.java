package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.puppet.pcore.impl.Helpers.any;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class EnumType extends ScalarDataType {
	public static final EnumType DEFAULT = new EnumType(Collections.emptyList());

	private static ObjectType ptype;
	public final List<String> enums;

	EnumType(List<String> enums) {
		this.enums = enums;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@SuppressWarnings("unchecked")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::EnumType", "Pcore::ScalarType",
				asMap("values", arrayType(StringType.NOT_EMPTY)));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, enumTypeDispatcher(), (self) -> new Object[]{self.enums});
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		// An instance of an Enum is a String
		return StringType.ITERABLE_TYPE;
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof String && enums.isEmpty() || any(enums, (en) -> en.equals(o));
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(enums.isEmpty())
			return type instanceof StringType || type instanceof EnumType || type instanceof PatternType;

		if(type instanceof StringType) {
			String value = ((StringType)type).value;
			return value != null && enums.contains(value);
		}

		if(type instanceof EnumType) {
			Collection<String> oEnums = ((EnumType)type).enums;
			return !oEnums.isEmpty() && enums.containsAll(oEnums);
		}
		return false;
	}

	@Override
	AnyType notAssignableCommon(AnyType other) {
		if(other instanceof StringType) {
			String value = ((StringType)other).value;
			if(value != null) {
				List<String> all = new ArrayList<>(enums);
				all.add(value);
				return enumType(all);
			}
			return stringType((IntegerType)TypeCalculator.SINGLETON.reduceType(map(enums, str -> {
				int sz = str.length();
				return integerType(sz, sz);
			})));
		}
		return super.notAssignableCommon(other);
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return enumType(Helpers.mergeUnique(enums, ((EnumType)other).enums));
	}
}
