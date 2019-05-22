package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.TypeConversionException;
import com.puppet.pcore.UndefinedValueException;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.types.IntegerType.DEFAULT_RADIX;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;

public class NumericType extends ScalarDataType {
	static final NumericType DEFAULT = new NumericType();

	private static ObjectType ptype;

	static Number fromHash(Map<String, Object> hash) {
		return fromArgs(hash.get("from"), (Boolean)hash.get("abs"));
	}

	static Number fromArgs(Object from, Boolean abs) {
		Number result = fromConvertible(from);
		if(abs != null && abs) {
			if(result instanceof Double)
				result = Math.abs(result.doubleValue());
			else
				result = Math.abs(result.longValue());
		}
		return result;
	}

	static Number fromString(String str) {
		if(str == null || str.isEmpty())
			return null;

		Number val = IntegerType.fromString(str, DEFAULT_RADIX);
		if(val != null)
			return val;

		try {
			return Double.valueOf(str);
		} catch(NumberFormatException e2) {
			return null;
		}
	}

	static Number fromConvertible(Object from) {
		if(from == null)
			throw new UndefinedValueException();
		if(from instanceof Long || from instanceof Double)
			return (Number)from;
		if(from instanceof Float)
			return ((Number)from).doubleValue();
		if(from instanceof Number)
			return ((Number)from).longValue();
		if(from instanceof Duration)
			return (double)((Duration)from).toNanos() / 1000000000.0;
		if(from instanceof Instant)
			return (double)((Instant)from).getEpochSecond() + (double)((Instant)from).getNano() / 1000000000.0;
		if(from instanceof Boolean)
			return ((boolean)from) ? 1 : 0;
		if(from instanceof String) {
			Number val = fromString((String)from);
			if(val != null)
				return val;
		}
		throw new TypeConversionException(format("Value of type '%s' cannot be converted to a Numeric", infer(from).generalize()));
	}

	NumericType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryDispatcher<T> factoryDispatcher() {
		AnyType convertible = variantType(undefType(), integerType(), floatType(), booleanType(), stringType(), timeSpanType(), timestampType());
		AnyType namedArgs = structType(structElement("from", convertible), structElement(optionalType("abs"), booleanType()));

		return (FactoryDispatcher<T>)dispatcher(
				constructor(
						(args) -> fromArgs(args.get(0), false),
						convertible),
				constructor(
						(args) -> fromArgs(args.get(0), (Boolean)args.get(1)),
						convertible, booleanType()),
				constructor(
						(args) -> fromHash((Map<String,Object>)args.get(0)),
						namedArgs)
		);
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::NumericType", "Pcore::ScalarType");
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, numericTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Number;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof NumericType;
	}
}
