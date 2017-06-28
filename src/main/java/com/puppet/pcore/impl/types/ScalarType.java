package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class ScalarType extends AnyType {
	public static final ScalarType DEFAULT = new ScalarType();

	private static ObjectType ptype;

	ScalarType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(ScalarType.class, "Pcore::ScalarType", "Pcore::AnyType", (args) -> DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof ScalarType;
	}
}
