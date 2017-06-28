package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.undefTypeDispatcher;

public class UndefType extends AnyType {
	static final UndefType DEFAULT = new UndefType();

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
		return ptype = pcore.createObjectType("Pcore::UndefType", "Pcore::AnyType");
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, undefTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o == null;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof UndefType;
	}
}
