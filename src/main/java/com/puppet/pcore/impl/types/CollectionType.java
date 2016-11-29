package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.singletonMap;

public class CollectionType extends TypeContainerType {
	public static final CollectionType DEFAULT = new CollectionType(AnyType.DEFAULT, IntegerType.POSITIVE);

	private static ObjectType ptype;
	public final IntegerType size;

	CollectionType(AnyType elementType, IntegerType size) {
		this(elementType, size, false);
	}

	CollectionType(AnyType elementType, IntegerType size, boolean resolved) {
		super(elementType, resolved);
		this.size = size.asSize();
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		return super.equals(o) && Objects.equals(size, ((CollectionType)o).size);
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new CollectionType(type.generalize(), (IntegerType)size.generalize());
	}

	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(size);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(CollectionType.class, "Pcore::CollectionType", "Pcore::AnyType",
				singletonMap("size_type", asMap(
						KEY_TYPE, typeType(IntegerType.POSITIVE),
						KEY_VALUE, IntegerType.POSITIVE)),
				(attrs) -> collectionType((IntegerType)attrs.get(0)),
				(self) -> new Object[]{self.size});
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		return iterableType(type);
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new CollectionType(type, size, resolved);
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof CollectionType)
			return size.isAssignable(((CollectionType)type).size, guard);
		if(type instanceof TupleType)
			return size.isAssignable(((TupleType)type).givenOrActualSize, guard);
		return type instanceof StructType && size.isAssignable(((StructType)type).size, guard);
	}
}
