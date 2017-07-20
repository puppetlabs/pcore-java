package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.ArrayType.arrayFactoryDispatcher;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class TupleType extends TypesContainerType {
	public static final TupleType DEFAULT = new TupleType(Collections.emptyList(), IntegerType.POSITIVE);
	public static final TupleType EXPLICIT_EMPTY = new TupleType(Collections.emptyList(), IntegerType.ZERO_SIZE);

	private static ObjectType ptype;
	public final IntegerType givenOrActualSize;
	public final IntegerType size;

	private TupleType(List<AnyType> types, IntegerType size, boolean resolved) {
		super(types, resolved);
		if(size == null) {
			long sz = types.size();
			this.givenOrActualSize = integerType(sz, sz);
			this.size = null;
		} else {
			this.size = size.asSize();
			this.givenOrActualSize = this.size;
		}
	}

	TupleType(List<AnyType> types, IntegerType size) {
		this(types, size, false);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public FactoryDispatcher<List<?>> factoryDispatcher() {
		return arrayFactoryDispatcher();
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(size);
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::TupleType", "Pcore::AnyType",
				asMap(
						"types", arrayType(typeType()),
						"size_type", asMap(
								KEY_TYPE, optionalType(typeType(IntegerType.POSITIVE)),
								KEY_VALUE, null)));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, tupleTypeDispatcher(),
				(self) -> new Object[]{self.types, self.size});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		if(size != null)
			size.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		return iterableType(variantType(types));
	}

	@Override
	TypesContainerType copyWith(List<AnyType> types, boolean resolved) {
		return new TupleType(types, size, true);
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && equals(size, ((TupleType)o).size, guard);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof List<?>) {
			List<?> lo = (List<?>)o;
			int oSize = lo.size();
			if(givenOrActualSize.isInstance(oSize, guard)) {
				int last = types.size() - 1;
				if(last >= 0) {
					for(int idx = 0, tdx = 0; idx < oSize; ++idx) {
						if(!types.get(tdx).isInstance(lo.get(idx), guard))
							return false;
						if(tdx < last)
							++tdx;
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof TupleType) {
			TupleType tt = (TupleType)t;
			if(!givenOrActualSize.isAssignable(tt.givenOrActualSize, guard))
				return false;
			if(!types.isEmpty()) {
				List<AnyType> tTypes = tt.types;
				int top = tTypes.size();
				int last = types.size() - 1;
				if(top == 0)
					return givenOrActualSize.min == 0;
				for(int idx = 0; idx < top; ++idx) {
					int myIdx = idx > last ? last : idx;
					if(!types.get(myIdx).isAssignable(tTypes.get(idx), guard))
						return false;
				}
			}
			return true;
		}
		if(t instanceof ArrayType) {
			ArrayType at = (ArrayType)t;
			if(!givenOrActualSize.isAssignable(at.size))
				return false;
			int top = types.size();
			if(top > 0) {
				int last = top - 1;
				for(int idx = 0; idx < top; ++idx) {
					int myIdx = idx > last ? last : idx;
					if(!types.get(myIdx).isAssignable(at.type, guard))
						return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		TupleType tt = (TupleType)other;
		return arrayType(TypeCalculator.SINGLETON.reduceType(Helpers.mergeUnique(types, tt.types)));
	}
}
