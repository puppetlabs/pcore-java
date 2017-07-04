package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.time.DurationFormat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Options.get;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static com.puppet.pcore.impl.types.TypeFactory.arrayType;
import static com.puppet.pcore.impl.types.TypeFactory.stringType;

public class TimeSpanType extends TimeDataType<TimeSpanType,Duration> {
	public static final Duration MAX_DURATION = Duration.ofSeconds(Long.MAX_VALUE, 999999999);
	public static final Duration MIN_DURATION = MAX_DURATION.negated();
	public static final TimeSpanType DEFAULT = new TimeSpanType(MIN_DURATION, MAX_DURATION);

	private static ObjectType ptype;

	TimeSpanType(Duration min, Duration max) {
		super(min, max);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public boolean isUnbounded() {
		return min.equals(MIN_DURATION) && max.equals(MAX_DURATION);
	}

	public boolean roundtripWithString() {
		return true;
	}

	@Override
	public FactoryDispatcher<Duration> factoryDispatcher() {
		AnyType formatType = stringType(2);
		return dispatcher(
				constructor(
						(args) -> fromSeconds((Number)args.get(0)),
						variantType(integerType(), floatType())),
				constructor(
						(args) -> DurationFormat.defaultParse((String)args.get(0)),
						stringType()),
				constructor(
						(args) -> DurationFormat.parse((String)args.get(0), (String)args.get(1)),
						stringType(), formatType),
				constructor(
						(args) -> DurationFormat.parse((String)args.get(0), (List<String>)args.get(1)),
						stringType(), arrayType(formatType)),
				constructor(
						(args) -> fromFields((List<Number>)args),
						tupleType(asList(integerType()), 4, 7)),
				constructor(
						(args) -> fromFieldsHash((Map<String, Object>)args.get(0)),
						structType(
								structElement(optionalType("negative"), booleanType()),
								structElement(optionalType("days"), integerType()),
								structElement(optionalType("hours"), integerType()),
								structElement(optionalType("minutes"), integerType()),
								structElement(optionalType("seconds"), integerType()),
								structElement(optionalType("milliseconds"), integerType()),
								structElement(optionalType("microseconds"), integerType()),
								structElement(optionalType("nanoseconds"), integerType())
						)),
				constructor(
						(args) -> fromStringHash((Map<String, Object>)args.get(0)),
						structType(
								structElement("string", stringType()),
								structElement(optionalType("format"), variantType(formatType, arrayType(formatType)))
						))
		);
	}

	public Duration fromStringHash(Map<String, Object> hash) {
		String str = get(hash, "string", String.class);
		Object formats = hash.get("format");
		if(formats == null)
			return DurationFormat.defaultParse(str);
		if(formats instanceof String)
			return DurationFormat.parse(str, (String)formats);
		if(formats instanceof List<?>)
			return DurationFormat.parse(str, (List<String>)formats);
		throw new IllegalArgumentException("TimeSpan format can not be a " + formats.getClass().getName());
	}

	public Duration fromSeconds(Number number) {
		return number instanceof Float || number instanceof Double
			? Duration.ofNanos((long)(number.doubleValue() * 1000000000.0))
		  : Duration.ofSeconds(number.longValue());
	}

	public Duration fromFieldsHash(Map<String, Object> fields) {
		Duration result = fromFields(asList(
				get(fields, "days", 0L),
				get(fields, "hours", 0L),
				get(fields, "minutes", 0L),
				get(fields, "seconds", 0L),
				get(fields, "milliseconds", 0L),
				get(fields, "microseconds", 0L),
				get(fields, "nanoseconds", 0L)
		));
		if(get(fields, "negative", false))
			result = result.negated();
		return result;
	}

	public Duration fromFields(List<Number> fields) {
		int nfields = fields.size();
		if(nfields < 4)
			throw new IllegalArgumentException("fromFields() requires at least four arguments");

		Duration result = Duration.of(fields.get(0).longValue(), ChronoUnit.DAYS)
				.plusHours(fields.get(1).longValue())
				.plusMinutes(fields.get(2).longValue())
				.plusSeconds(fields.get(3).longValue());

		if(nfields > 4) {
			result = result.plusMillis(fields.get(4).longValue());
			if(nfields > 5) {
				result = result.plusNanos(fields.get(5).longValue() * 1000);
				if(nfields > 6)
					result = result.plusNanos(fields.get(6).longValue());
			}
		}
		return result;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Duration) {
			Duration io = (Duration)o;
			return io.compareTo(min) >= 0 && io.compareTo(max) <= 0;
		}
		return false;
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::TimeSpanType", "Pcore::ScalarType",
				asMap(
						"min", asMap(
								KEY_TYPE, timeSpanType(),
								KEY_VALUE, MIN_DURATION),
						"max", asMap(
								KEY_TYPE, timeSpanType(),
								KEY_VALUE, MAX_DURATION)));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, timeSpanTypeDispatcher(),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	TimeSpanType newInstance(Duration min, Duration max) {
		return timeSpanType(min, max);
	}
}
