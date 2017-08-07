package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
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
import static java.lang.String.format;

public class IntegerType extends NumericType implements MergableRange<IntegerType> {
	static final IntegerType DEFAULT = new IntegerType(Long.MIN_VALUE, Long.MAX_VALUE);
	static final IntegerType POSITIVE = new IntegerType(0, Long.MAX_VALUE);
	static final IntegerType ZERO_SIZE = new IntegerType(0, 0);
	static final IntegerType ONE = new IntegerType(1, 1);

	static final int DEFAULT_RADIX = 0;

	private static ObjectType ptype;
	public final long max;
	public final long min;

	static Long fromString(final String str, final int givenRadix) {
		if(str == null || str.isEmpty())
			return null;

		try {
				int radix;
				int len = str.length();
				int start = 0;
				char first = str.charAt(0);
				boolean negative = first == '-';
				if(negative || first == '+') {
					++start;

					// Accept whitespace between sign and first digit
					while(start < len && Character.isWhitespace(str.charAt(start)))
						++start;
				}
				if(start >= len)
					return null;

				if(len - start > 1 && str.charAt(start) == '0') {
					++start;
					char x = str.charAt(start);
					if(x == 'x' || x == 'X') {
						radix = 16;
						++start;
					} else if(x == 'b' || x == 'B') {
						radix = 2;
						++start;
					} else {
						radix = 8;
					}
					if(givenRadix != DEFAULT_RADIX && radix != givenRadix)
						throw new TypeConversionException(format("Given radix %d conflicts with radix %d implied by string '%s'", givenRadix, radix, str));
				} else
					radix = givenRadix == DEFAULT_RADIX ? 10 : assertRadix(givenRadix);

				long value = Long.parseLong(start > 0 ? str.substring(start) : str, radix);
				return negative ? -value : value;
		} catch(NumberFormatException e2) {
			return null;
		}
	}

	static Long fromHash(Map<String, Object> hash) {
		return fromArgs(hash.get("from"), hash.get("radix"), (Boolean)hash.get("abs"));
	}

	static Long fromArgs(Object from, Object radixObj, Boolean abs) {
		Long result = fromConvertible(from, radixObj == null || radixObj == Default.SINGLETON ? DEFAULT_RADIX : ((Number)radixObj).intValue());
		if(abs != null && abs)
			result = Math.abs(result);
		return result;
	}

	static Long fromConvertible(Object from, int radix) {
		if(from == null)
			throw new UndefinedValueException();
		if(from instanceof String) {
			Long val = fromString((String)from, radix);
			if(val != null)
				return val;
		} else {
			if(from instanceof Long)
				return (Long)from;
			if(from instanceof Number)
				return ((Number)from).longValue();
			if(from instanceof Duration)
				return ((Duration)from).toMillis() / 1000;
			if(from instanceof Instant)
				return ((Instant)from).getEpochSecond();
			if(from instanceof Boolean)
				return ((boolean)from) ? 1L : 0L;
		}
		throw new TypeConversionException(format("Value of type '%s' cannot be converted to an Integer", infer(from).generalize()));
	}

	private static int assertRadix(int radix) {
		switch(radix) {
		case DEFAULT_RADIX:
		case 2:
		case 8:
		case 10:
		case 16:
				break;
		default:
			throw new TypeConversionException(format("Illegal radix: '%d', expected 2, 8, 10, 16, or default", radix));
		}
		return radix;
	}

	IntegerType(long min, long max) {
		assertMinMax(min, max, () -> "IntegerType parameters");
		this.min = min;
		this.max = max;
	}

	@Override
	public Type _pcoreType() {
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

	@SuppressWarnings("unchecked")
	@Override
	public FactoryDispatcher<Long> factoryDispatcher() {
		AnyType radixType = variantType(defaultType(), integerType(2, 16));
		AnyType convertibleType = variantType(undefType(), numericType(), booleanType(), stringType(), timeSpanType(), timestampType());
		AnyType namedArgs = structType(structElement("from", convertibleType), structElement(optionalType("radix"), radixType), structElement(optionalType("abs"), booleanType()));

		return dispatcher(
				constructor(
						(args) -> fromArgs(args.get(0), Default.SINGLETON, false),
						convertibleType),
				constructor(
						(args) -> fromArgs(args.get(0), args.get(1), false),
						convertibleType, radixType),
				constructor(
						(args) -> fromArgs(args.get(0), args.get(1), (Boolean)args.get(2)),
						convertibleType, radixType, booleanType()),
				constructor(
						(args) -> fromHash((Map<String,Object>)args.get(0)),
						namedArgs)
		);
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
		return ptype = pcore.createObjectType("Pcore::IntegerType", "Pcore::NumericType",
				asMap(
						"from", asMap(
								KEY_TYPE, integerType(),
								KEY_VALUE, Long.MIN_VALUE),
						"to", asMap(
								KEY_TYPE, integerType(),
								KEY_VALUE, Long.MAX_VALUE)));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, integerTypeDispatcher(),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o instanceof IntegerType) {
			IntegerType io = (IntegerType)o;
			return min == io.min && max == io.max;
		}
		return false;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte) {
			long v = ((Number)o).longValue();
			return v >= min && v <= max;
		}
		return false;
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
