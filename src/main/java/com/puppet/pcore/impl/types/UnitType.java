package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.unitTypeDispatcher;

public class UnitType extends AnyType {
	public static final UnitType DEFAULT = new UnitType();

	private static ObjectType ptype;

	private UnitType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType common(Type other) {
		return (AnyType)other;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return true;
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::UnitType", "Pcore::AnyType");
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, unitTypeDispatcher());
	}
}
