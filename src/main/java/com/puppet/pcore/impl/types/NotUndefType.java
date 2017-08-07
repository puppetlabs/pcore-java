package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class NotUndefType extends TypeContainerType {
	static final NotUndefType DEFAULT = new NotUndefType(AnyType.DEFAULT);

	private static ObjectType ptype;

	NotUndefType(AnyType type) {
		this(type, false);
	}

	private NotUndefType(AnyType type, boolean resolved) {
		super(type, resolved);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType actualType() {
		return type.actualType();
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new NotUndefType(type.generalize());
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::NotUndefType", "Pcore::AnyType",
				asMap(
						"type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, notUndefTypeDispatcher(),
				(self) -> new Object[]{self.type});
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o != null && type.isInstance(o, guard);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return !t.isAssignable(UndefType.DEFAULT, guard) && type.isAssignable(t, guard);
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new NotUndefType(type, resolved);
	}
}
