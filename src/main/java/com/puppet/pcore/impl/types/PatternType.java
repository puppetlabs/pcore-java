package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.any;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.singletonMap;

public class PatternType extends ScalarType {
	public static final PatternType DEFAULT = new PatternType(Collections.emptyList());

	private static ObjectType ptype;
	public final List<RegexpType> regexps;

	PatternType(List<RegexpType> regexps) {
		this.regexps = regexps;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return regexps.hashCode();
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::PatternType", "Pcore::ScalarType",
				singletonMap(
						"patterns", arrayType(variantType(typeType(regexpType()), regexpType()))));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, patternTypeDispatcher(),
				(self) -> new Object[]{self.regexps});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		for(RegexpType regexp : regexps)
			regexp.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o instanceof PatternType && Objects.equals(regexps, ((PatternType)o).regexps);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof String) {
			String so = (String)o;
			return regexps.isEmpty() || any(regexps, regexp -> regexp.matches(so));
		}
		return false;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(this == t)
			return true;

		if(t instanceof StringType) {
			if(regexps.isEmpty())
				return true;
			String value = ((StringType)t).value;
			return value != null && any(regexps, regexp -> regexp.matches(value));
		}

		if(t instanceof EnumType) {
			if(regexps.isEmpty())
				return true;
			List<String> enums = ((EnumType)t).enums;
			return !enums.isEmpty() && all(enums, value -> any(regexps, regexp -> regexp.matches(value)));
		}

		return t instanceof PatternType && (regexps.isEmpty() || regexps.containsAll(((PatternType)t).regexps));

	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return patternType(Helpers.mergeUnique(regexps, ((PatternType)other).regexps));
	}
}
