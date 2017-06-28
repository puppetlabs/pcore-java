package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.MergableRange;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Assertions.assertMinMax;
import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.floatType;

public class FloatType extends NumericType implements MergableRange<FloatType> {
	public static final FloatType DEFAULT = new FloatType(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

	private static ObjectType ptype;
	public final double max;
	public final double min;

	FloatType(double min, double max) {
		assertMinMax(min, max, () -> "FloatType parameters");
		this.min = min;
		this.max = max;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public boolean equals(Object o) {
		if(o instanceof FloatType) {
			FloatType fo = (FloatType)o;
			return min == fo.min && max == fo.max;
		}
		return false;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return super.hashCode() + (int)min ^ (int)max;
	}

	@Override
	public boolean isOverlap(FloatType t) {
		return !(max < t.min || t.max < min);
	}

	public boolean isUnbounded() {
		return min == Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY;
	}

	@Override
	public FloatType merge(FloatType o) {
		return isOverlap(o)
				? new FloatType(min <= o.min ? min : o.min, max >= o.max ? max : o.max)
				: null;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(FloatType.class, "Pcore::FloatType", "Pcore::NumericType",
				asMap(
						"from", asMap(
								KEY_TYPE, floatType(),
								KEY_VALUE, Double.NEGATIVE_INFINITY),
						"to", asMap(
								KEY_TYPE, floatType(),
								KEY_VALUE, Double.POSITIVE_INFINITY)),
				(attrs) -> floatType((Double)attrs.get(0), (Double)attrs.get(1)),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		if(type instanceof FloatType) {
			FloatType ft = (FloatType)type;
			return min <= ft.min && max >= ft.max;
		}
		return false;
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		FloatType ft = (FloatType)other;
		return floatType(Math.min(min, ft.min), Math.max(max, ft.max));
	}
}
