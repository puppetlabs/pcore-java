package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.booleanTypeDispatcher;

public class BooleanType extends ScalarDataType {
	public static final BooleanType DEFAULT = new BooleanType();

	private static ObjectType ptype;

	private BooleanType() {
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
		return ptype = pcore.createObjectType("Pcore::BooleanType", "Pcore::ScalarType");
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, booleanTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Boolean;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof BooleanType;
	}
}
