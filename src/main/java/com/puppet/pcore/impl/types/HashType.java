package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Map;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;

public class HashType extends CollectionType {
	static final HashType DEFAULT = new HashType(anyType(), anyType(), IntegerType.POSITIVE);
	static final HashType EMPTY = new HashType(unitType(), unitType(), integerType(0, 0));
	static final IntegerType KEY_PAIR_TUPLE_SIZE = integerType(2, 2);
	static final AnyType DEFAULT_KEY_PAIR_TUPLE = tupleType(
			asList(AnyType.DEFAULT, AnyType.DEFAULT),
			KEY_PAIR_TUPLE_SIZE);

	private static ObjectType ptype;
	public final AnyType keyType;

	HashType(AnyType keyType, AnyType valueType, IntegerType size) {
		this(keyType, valueType, size, false);
	}

	private HashType(AnyType keyType, AnyType valueType, IntegerType size, boolean resolved) {
		super(valueType, size, resolved);
		this.keyType = keyType;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public IterableType asIterableType(RecursionGuard guard) {
		if(DEFAULT.equals(this) || EMPTY.equals(this))
			return iterableType(variantType(DEFAULT_KEY_PAIR_TUPLE));
		return iterableType(tupleType(asList(keyType, type), KEY_PAIR_TUPLE_SIZE));
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT)
				? this
				: new HashType(keyType.generalize(), type.generalize(), (IntegerType)size.generalize());
	}

	public int hashCode() {
		return super.hashCode() * 31 + keyType.hashCode();
	}

	@Override
	public boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	protected void accept(Visitor visitor, RecursionGuard guard) {
		keyType.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	@Override
	protected AnyType copyWith(AnyType type, boolean resolved) {
		return new HashType(keyType, type, size, resolved);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Map<?,?>) {
			Map<?,?> mo = (Map<?,?>)o;
			return size.isInstance(mo.size()) && (DEFAULT.equals(this) || all(mo.entrySet(),
					(entry) -> keyType.isInstance(entry.getKey()) && type.isInstance(entry.getValue())));
		}
		return false;
	}

	@Override
	protected boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof HashType) {
			HashType ht = (HashType)t;
			return size.min == 0 && EMPTY.equals(ht)
					|| super.isUnsafeAssignable(t, guard) && type.isAssignable(ht.type, guard) && keyType.isAssignable(ht.keyType, guard);
		}

		if(t instanceof StructType) {
			// hash must accept all actual key types
			// hash must accept all value types
			// hash must accept the attributeCount of the struct
			StructType st = (StructType)t;
			return size.isAssignable(st.size, guard) &&
					all(st.elements, member -> keyType.isAssignable(member.key.actualType(), guard) && type.isAssignable(member.value, guard));
		}
		return false;
	}

	@Override
	protected AnyType notAssignableSameClassCommon(AnyType other) {
		HashType ht = (HashType)other;
		return hashType(keyType.common(ht.keyType), type.common(ht.type));
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && keyType.guardedEquals(((HashType)o).keyType, guard);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::HashType", "Pcore::CollectionType",
				asMap(
						"key_type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType()),
						"value_type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())),
				asList("key_type", "value_type", "size_type"));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, hashTypeDispatcher(),
				(self) -> new Object[]{self.keyType, self.type, self.size});
	}
}
