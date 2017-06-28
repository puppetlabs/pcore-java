package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class TypeType extends TypeContainerType {
	public static final TypeType DEFAULT = new TypeType(AnyType.DEFAULT);

	private static ObjectType ptype;

	TypeType(AnyType type) {
		this(type, false);
	}

	private TypeType(AnyType type, boolean resolved) {
		super(type, false);
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
		return ptype = pcore.createObjectType(TypeType.class, "Pcore::TypeType", "Pcore::AnyType",
				asMap(
						"type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())),
				(args) -> typeType((AnyType)args.get(0)),
				(self) -> new Object[]{self.type});
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		if(type instanceof EnumType)
			// type describes the element type perfectly since the iteration is made over the
			// contained choices.
			return iterableType(type);
		if(type instanceof IntegerType)
			// type describes the element type perfectly since the iteration is made over the
			// specified range.
			return ((IntegerType)type).isFiniteRange() ? iterableType(type) : null;
		return null;
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new TypeType(type, resolved);
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return type instanceof EnumType || type instanceof IntegerType && ((IntegerType)type).isFiniteRange();
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof TypeType && type.isAssignable(((TypeType)t).type, guard);
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return typeType(type.common(((TypeType)other).type));
	}
}
