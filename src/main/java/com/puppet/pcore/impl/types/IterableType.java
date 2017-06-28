package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class IterableType extends TypeContainerType {
	public static final IterableType DEFAULT = new IterableType(AnyType.DEFAULT);

	private static ObjectType ptype;

	IterableType(AnyType elementType) {
		this(elementType, false);
	}

	private IterableType(AnyType elementType, boolean resolved) {
		super(elementType, resolved);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public IterableType asIterableType(RecursionGuard guard) {
		return this;
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new IterableType(type.generalize());
	}

	@Override
	public boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	protected AnyType copyWith(AnyType type, boolean resolved) {
		return new IterableType(type, resolved);
	}

	@Override
	protected boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(type.isAssignable(AnyType.DEFAULT, guard))
			// Don't request the iterable_type. Since this Iterable accepts Any element, it is enough that o is iterable.
			return t.isIterable(guard);

		IterableType it = t.asIterableType(guard);
		return it != null && type.isAssignable(it.type, guard);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(IterableType.class, "Pcore::IterableType", "Pcore::AnyType",
				asMap(
						"element_type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())),
				(args) -> iterableType((AnyType)args.get(0)),
				(self) -> new Object[]{self.type});
	}
}
