package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class NumericType extends ScalarDataType {
	public static final NumericType DEFAULT = new NumericType();

	private static ObjectType ptype;

	NumericType() {
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(NumericType.class, "Pcore::NumericType", "Pcore::ScalarType", (args) ->
				DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof NumericType;
	}
}
