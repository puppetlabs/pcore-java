package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.*;

public class ScalarDataType extends ScalarType {
	static final ScalarDataType DEFAULT = new ScalarDataType();

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
		return ptype = pcore.createObjectType("Pcore::ScalarDataType", "Pcore::ScalarType");
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, scalarDataTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o == null || o instanceof String || o instanceof Number || o instanceof Boolean;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return stringType().isAssignable(t, guard)
				|| integerType().isAssignable(t, guard)
				|| booleanType().isAssignable(t, guard)
				|| floatType().isAssignable(t, guard);
	}
}
