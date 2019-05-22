package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;
import com.puppet.pcore.time.InstantFormat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Options.get;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class TimestampType extends TimeDataType<TimestampType,Instant> {
	static final TimestampType DEFAULT = new TimestampType(Instant.MIN, Instant.MAX);

	private static ObjectType ptype;

	TimestampType(Instant min, Instant max) {
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
		return min.equals(Instant.MIN) && max.equals(Instant.MAX);
	}

	@Override
	public boolean roundtripWithString() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryDispatcher<T> factoryDispatcher() {
		AnyType formatType = stringType(2);
		return (FactoryDispatcher<T>)dispatcher(
				constructor(
						(args) -> Instant.now()),
				constructor(
						(args) -> InstantFormat.SINGLETON.parse((String)args.get(0)),
						stringType()),
				constructor(
						(args) -> InstantFormat.SINGLETON.parse((String)args.get(0), (String)args.get(1)),
						stringType(), formatType),
				constructor(
						(args) -> InstantFormat.SINGLETON.parse((String)args.get(0), (List<String>)args.get(1)),
						stringType(), arrayType(formatType)),
				constructor(
						(args) -> InstantFormat.SINGLETON.parse((String)args.get(0), (List<String>)args.get(1), (String)args.get(2)),
						stringType(), arrayType(formatType), stringType(1)),
				constructor(
						(args) -> fromSeconds((Number)args.get(0)),
						variantType(integerType(), floatType())),
				constructor(
						(args) -> fromStringHash((Map<String, Object>)args.get(0)),
						structType(
								structElement("string", stringType(1)),
								structElement(optionalType("format"), variantType(formatType, arrayType(formatType))),
								structElement(optionalType("timezone"), stringType(1))
						))
		);
	}

	public Instant fromSeconds(Number seconds) {
		return seconds instanceof Float || seconds instanceof Double
				? Instant.ofEpochSecond(seconds.longValue(), (long)(seconds.doubleValue() - Math.floor(seconds.doubleValue())) * 1000000000)
				: Instant.ofEpochSecond(seconds.longValue());
	}

	@SuppressWarnings("unchecked")
	public Instant fromStringHash(Map<String, Object> hash) {
		String str = get(hash, "string", String.class);
		String tz = get(hash, "timezone", (String)null);
		Object formats = hash.get("format");
		if(formats == null) {
			return tz == null
					? InstantFormat.SINGLETON.parse(str)
					: InstantFormat.SINGLETON.parse(str, InstantFormat.DEFAULTS_WO_TZ, ZoneId.of(tz));
		}
		if(formats instanceof String)
			return InstantFormat.SINGLETON.parse(str, (String)formats, tz);
		if(formats instanceof List<?>)
			return InstantFormat.SINGLETON.parse(str, (List<String>)formats, tz);
		throw new IllegalArgumentException("Timestamp format can not be a " + formats.getClass().getName());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Instant) {
			Instant io = (Instant)o;
			return io.compareTo(min) >= 0 && io.compareTo(max) <= 0;
		}
		return false;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::TimestampType", "Pcore::ScalarType",
				asMap(
						"min", asMap(
								KEY_TYPE, timestampType(),
								KEY_VALUE, Instant.MIN),
						"max", asMap(
								KEY_TYPE, timestampType(),
								KEY_VALUE, Instant.MAX)));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, timestampTypeDispatcher(),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	TimestampType newInstance(Instant min, Instant max) {
		return timestampType(min, max);
	}
}
