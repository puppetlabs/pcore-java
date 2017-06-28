package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class UndefType extends AnyType {
	public static final UndefType DEFAULT = new UndefType();

	private static ObjectType ptype;

	private UndefType() {
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
		return ptype = pcore.createObjectType(UndefType.class, "Pcore::UndefType", "Pcore::AnyType", (args) -> DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof UndefType;
	}
}
