package com.puppet.pcore.impl.types;

import com.puppet.pcore.Default;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.defaultTypeDispatcher;

public class DefaultType extends AnyType {
	public static final DefaultType DEFAULT = new DefaultType();

	private static ObjectType ptype;

	private DefaultType() {
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
		return ptype = pcore.createObjectType("Pcore::DefaultType", "Pcore::AnyType");
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, defaultTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Default;
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return type instanceof DefaultType;
	}
}
