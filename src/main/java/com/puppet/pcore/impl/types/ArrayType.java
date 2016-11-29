package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Assertions.assertNotNull;
import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class ArrayType extends CollectionType {
	public static final ArrayType DATA = new ArrayType(DataType.DEFAULT, IntegerType.POSITIVE);
	public static final ArrayType DEFAULT = new ArrayType(AnyType.DEFAULT, IntegerType.POSITIVE);
	public static final ArrayType EMPTY = new ArrayType(UnitType.DEFAULT, IntegerType.ZERO_SIZE);

	private static ObjectType ptype;

	ArrayType(AnyType elementType, IntegerType size) {
		this(assertNotNull(elementType, () -> "Array element type"), assertNotNull(size, () -> "Array attributeCount type"), false);
	}

	ArrayType(AnyType elementType, IntegerType size, boolean resolved) {
		super(elementType, size, resolved);
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new ArrayType(type.generalize(), IntegerType.POSITIVE);
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(ArrayType.class, "Pcore::ArrayType", "Pcore::CollectionType",
				singletonMap("element_type", asMap(
						KEY_TYPE, typeType(),
						KEY_VALUE, anyType())),
				asList("element_type", "size_type"),
				(attrs) -> arrayType((AnyType)attrs.get(0), (IntegerType)attrs.get(1)),
				(self) -> new Object[]{self.type, self.size});
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new ArrayType(type, size, resolved);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof TupleType)
			return super.isUnsafeAssignable(t, guard)
					&& ((TupleType)t).types.stream().allMatch(tt -> type.isAssignable(tt, guard));

		return t instanceof ArrayType && super.isUnsafeAssignable(t, guard) && type.isAssignable(
				((ArrayType)t).type,
				guard);
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return arrayType(type.common(((ArrayType)other).type));
	}
}
