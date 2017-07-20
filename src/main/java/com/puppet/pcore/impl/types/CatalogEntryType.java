package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.types.TypeFactory.catalogEntryTypeDispatcher;
import static com.puppet.pcore.impl.types.TypeFactory.infer;

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
		return ptype = pcore.createObjectType("Pcore::CatalogEntryType", "Pcore::AnyType");
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, catalogEntryTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return isAssignable(infer(o), guard);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return type instanceof CatalogEntryType;
	}
}
