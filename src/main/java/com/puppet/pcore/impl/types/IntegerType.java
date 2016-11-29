package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.TypeAssertionException;
import com.puppet.pcore.impl.MergableRange;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Assertions.assertMinMax;
import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.integerType;
import static com.puppet.pcore.impl.types.TypeFactory.iterableType;
import static java.lang.String.format;

public class IntegerType extends NumericType implements MergableRange<IntegerType> {
	public static final IntegerType DEFAULT = new IntegerType(Long.MIN_VALUE, Long.MAX_VALUE);
	public static final IntegerType POSITIVE = new IntegerType(0, Long.MAX_VALUE);
	public static final IntegerType ZERO_SIZE = new IntegerType(0, 0);

	private static ObjectType ptype;
	public final long max;
	public final long min;

	IntegerType(long min, long max) {
		assertMinMax(min, max, () -> "IntegerType parameters");
		this.min = min;
		this.max = max;
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	@Override
	public IterableType asIterableType(RecursionGuard guard) {
		// It's unknown if the iterable will be a range (min, max) or a "times" (0, max)
		return iterableType(integerType());
	}

	public IntegerType asSize() {
		if(min < 0) {
			if(min != Long.MIN_VALUE)
				throw new TypeAssertionException(format("%s lower bound cannot be used as a attributeCount", this));
			return integerType(0, max);
		}
		return this;
	}

	public boolean equals(Object o) {
		if(o instanceof IntegerType) {
			IntegerType io = (IntegerType)o;
			return min == io.min && max == io.max;
		}
		return false;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return super.hashCode() + (int)(min ^ max);
	}

	public boolean isFiniteRange() {
		return min != Long.MIN_VALUE && max != Long.MAX_VALUE;
	}

	@Override
	public boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	public boolean isOverlap(IntegerType t) {
		return !(max < t.min || t.max < min);
	}

	public boolean isUnbounded() {
		return min == Long.MIN_VALUE && max == Long.MAX_VALUE;
	}

	@Override
	public IntegerType merge(IntegerType o) {
		return isOverlap(o) || isAdjacent(o)
				? new IntegerType(min <= o.min ? min : o.min, max >= o.max ? max : o.max)
				: null;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(IntegerType.class, "Pcore::IntegerType", "Pcore::NumericType",
				asMap(
						"from", asMap(
								KEY_TYPE, integerType(),
								KEY_VALUE, Long.MIN_VALUE),
						"to", asMap(
								KEY_TYPE, integerType(),
								KEY_VALUE, Long.MAX_VALUE)),
				(args) -> integerType((Long)args.get(0), (Long)args.get(1)),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof IntegerType) {
			IntegerType ft = (IntegerType)type;
			return min <= ft.min && max >= ft.max;
		}
		return false;
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		IntegerType it = (IntegerType)other;
		return integerType(Math.min(min, it.min), Math.max(max, it.max));
	}

	private boolean isAdjacent(IntegerType t) {
		return max + 1 == t.min || t.max + 1 == min;
	}
}
