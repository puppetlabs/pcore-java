package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class OptionalType extends TypeContainerType {
	public static final OptionalType DEFAULT = new OptionalType(AnyType.DEFAULT);

	private static ObjectType ptype;

	OptionalType(AnyType type) {
		this(type, false);
	}

	private OptionalType(AnyType type, boolean resolved) {
		super(type, false);
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	@Override
	public AnyType actualType() {
		return type.actualType();
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new OptionalType(type.generalize());
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(OptionalType.class, "Pcore::OptionalType", "Pcore::AnyType",
				asMap(
						"type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())),
				(args) -> optionalType((AnyType)args.get(0)),
				(self) -> new Object[]{self.type});
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new OptionalType(type, resolved);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t.isAssignable(UndefType.DEFAULT, guard) || type.isAssignable(t, guard);
	}
}
