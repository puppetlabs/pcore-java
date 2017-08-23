package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.singletonMap;

public class CollectionType extends TypeContainerType {
	static final CollectionType DEFAULT = new CollectionType(anyType(), integerType());

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
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new CollectionType(type.generalize(), (IntegerType)size.generalize());
	}

	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(size);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::CollectionType", "Pcore::AnyType",
				singletonMap("size_type", asMap(
						KEY_TYPE, typeType(IntegerType.POSITIVE),
						KEY_VALUE, IntegerType.POSITIVE)));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, collectionTypeDispatcher(),
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
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && equals(size, ((CollectionType)o).size, guard);
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return (o instanceof Map<?,?> && size.isInstance(((Map<?,?>)o).size()))
				|| (o instanceof List<?> && size.isInstance(((List<?>)o).size()));
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
