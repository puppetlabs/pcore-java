package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.booleanType;

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
		return ptype = pcore.createObjectType(BooleanType.class, "Pcore::BooleanType", "Pcore::ScalarType",
				(attrs) -> booleanType());
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof BooleanType;
	}
}
