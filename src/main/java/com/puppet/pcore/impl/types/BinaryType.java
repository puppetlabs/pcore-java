package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.binaryType;

public class BinaryType extends AnyType {
	public static final BinaryType DEFAULT = new BinaryType();

	private static ObjectType ptype;

	private BinaryType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(BinaryType.class, "Pcore::BinaryType", "Pcore::AnyType", (attrs) ->
				binaryType());
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof BinaryType;
	}
}
