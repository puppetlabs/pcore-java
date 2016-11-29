package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class DefaultType extends AnyType {
	public static final DefaultType DEFAULT = new DefaultType();

	private static ObjectType ptype;

	private DefaultType() {
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(DefaultType.class, "Pcore::DefaultType", "Pcore::AnyType", (attrs) ->
				DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return type instanceof DefaultType;
	}
}
