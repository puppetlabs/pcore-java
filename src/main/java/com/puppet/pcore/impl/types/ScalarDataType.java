package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class ScalarDataType extends ScalarType {
	public static final ScalarDataType DEFAULT = new ScalarDataType();

	private static ObjectType ptype;

	ScalarDataType() {
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
		return ptype = pcore.createObjectType(ScalarDataType.class, "Pcore::ScalarDataType", "Pcore::ScalarType", (args)
				-> DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof ScalarDataType;
	}
}
