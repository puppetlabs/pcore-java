package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

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

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(UnitType.class, "Pcore::UnitType", "Pcore::AnyType", (args) -> DEFAULT);
	}
}
