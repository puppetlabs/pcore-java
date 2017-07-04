package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class IteratorType extends TypeContainerType {
	public static final IteratorType DEFAULT = new IteratorType(AnyType.DEFAULT);

	private static ObjectType ptype;

	IteratorType(AnyType elementType) {
		this(elementType, false);
	}

	private IteratorType(AnyType elementType, boolean resolved) {
		super(elementType, resolved);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public IterableType asIterableType(RecursionGuard guard) {
		return iterableType(type);
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new IteratorType(type.generalize());
	}

	@Override
	public boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	protected AnyType copyWith(AnyType type, boolean resolved) {
		return new IteratorType(type, resolved);
	}

	@Override
	protected boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return (t instanceof IteratorType) && type.isAssignable(((IteratorType)t).type, guard);
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::IteratorType", "Pcore::AnyType",
				asMap(
						"element_type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, iteratorTypeDispatcher(),
				(self) -> new Object[]{self.type});
	}
}
