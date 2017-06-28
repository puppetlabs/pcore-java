package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

public class CatalogEntryType extends AnyType {
	public static final CatalogEntryType DEFAULT = new CatalogEntryType();

	private static ObjectType ptype;

	CatalogEntryType() {
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
		return ptype = pcore.createObjectType(CatalogEntryType.class, "Pcore::CatalogEntryType", "Pcore::AnyType", (attrs)
				-> DEFAULT);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return type instanceof CatalogEntryType;
	}
}
