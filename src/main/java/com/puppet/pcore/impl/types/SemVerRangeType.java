package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class SemVerRangeType extends ScalarType {
	public static final SemVerRangeType DEFAULT = new SemVerRangeType();

	private static ObjectType ptype;

	private SemVerRangeType() {
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
		return ptype = pcore.createObjectType(SemVerRangeType.class, "Pcore::SemVerRangeType", "Pcore::ScalarType", (args)
				-> DEFAULT);
	}
}
