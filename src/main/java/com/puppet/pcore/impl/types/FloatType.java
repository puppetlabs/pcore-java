package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.TypeConversionException;
import com.puppet.pcore.UndefinedValueException;
import com.puppet.pcore.impl.MergableRange;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.puppet.pcore.impl.Assertions.assertMinMax;
import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static com.puppet.pcore.impl.types.TypeFactory.booleanType;
import static java.lang.String.format;

public class FloatType extends NumericType implements MergableRange<FloatType> {
	public static final FloatType DEFAULT = new FloatType(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

	private static ObjectType ptype;

	static Double fromHash(Map<String, Object> hash) {
		return fromArgs(hash.get("from"), (Boolean)hash.get("abs"));
	}

	static Double fromArgs(Object from, Boolean abs) {
		Double result = fromConvertible(from);
		if(abs != null && abs)
			result = Math.abs(result);
		return result;
	}

	static Double fromString(String str) {
		if(str == null || str.isEmpty())
			return null;

		try {
			return Double.valueOf(str);
		} catch(NumberFormatException e2) {
			return null;
		}
	}

	static Double fromConvertible(Object from) {
		if(from == null)
			throw new UndefinedValueException();
		if(from instanceof Double)
			return (Double)from;
		if(from instanceof Number)
			return ((Number)from).doubleValue();
		if(from instanceof Duration)
			return (double)((Duration)from).toNanos() / 1000000000.0;
		if(from instanceof Instant)
			return (double)((Instant)from).getEpochSecond() + (double)((Instant)from).getNano() / 1000000000.0;
		if(from instanceof Boolean)
			return ((boolean)from) ? 1.0 : 0.0;
		if(from instanceof String) {
			Double val = fromString((String)from);
			if(val != null)
				return val;
		}
		throw new TypeConversionException(format("Value of type '%s' cannot be converted to a Float", infer(from).generalize()));
	}

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

	@Override
	public FactoryDispatcher<? extends Number> factoryDispatcher() {
		AnyType convertibleType = variantType(undefType(), numericType(), booleanType(), stringType(), timeSpanType(), timestampType());
		AnyType namedArgsType = structType(structElement("from", convertibleType), structElement(optionalType("abs"), booleanType()));

		return dispatcher(
				constructor(
						(args) -> fromArgs(args.get(0), false),
						convertibleType),
				constructor(
						(args) -> fromArgs(args.get(0), (Boolean)args.get(1)),
						convertibleType, booleanType()),
				constructor(
						(args) -> fromHash((Map<String,Object>)args.get(0)),
						namedArgsType)
		);
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
		return ptype = pcore.createObjectType("Pcore::FloatType", "Pcore::NumericType",
				asMap(
						"from", asMap(
								KEY_TYPE, floatType(),
								KEY_VALUE, Double.NEGATIVE_INFINITY),
						"to", asMap(
								KEY_TYPE, floatType(),
								KEY_VALUE, Double.POSITIVE_INFINITY)));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, floatTypeDispatcher(),
				(self) -> new Object[]{self.min, self.max});
	}

	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o instanceof FloatType) {
			FloatType fo = (FloatType)o;
			return min == fo.min && max == fo.max;
		}
		return false;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Float || o instanceof Double) {
			double v = ((Number)o).doubleValue();
			return v >= min && v <= max;
		}
		return false;
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
