package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class DataType extends AnyType {
	public static final DataType DEFAULT = new DataType();

	private static ObjectType ptype;

	private DataType() {
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
		return ptype = pcore.createObjectType(DataType.class, "Pcore::DataType", "Pcore::AnyType", (attrs) -> DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof DataType)
			return true;
		if(type instanceof NotUndefType)
			// We cannot put the NotUndefType[Data] in the VariantType.DATA since that causes an endless recursion
			return isAssignable(((NotUndefType)type).type, guard);
		return VariantType.DATA.isAssignable(type, guard);
	}
}
