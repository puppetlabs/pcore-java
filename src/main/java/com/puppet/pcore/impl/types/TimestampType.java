package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.time.Instant;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.timestampType;

public class TimestampType extends TimeDataType<TimestampType,Instant> {
	public static final TimestampType DEFAULT = new TimestampType(Instant.MIN, Instant.MAX);

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

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(TimestampType.class, "Pcore::TimestampType", "Pcore::ScalarType",
				asMap(
						"min", asMap(
								KEY_TYPE, timestampType(),
								KEY_VALUE, Instant.MIN),
						"max", asMap(
								KEY_TYPE, timestampType(),
								KEY_VALUE, Instant.MAX)),
				(args) -> timestampType((Instant)args.get(0), (Instant)args.get(1)),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	TimestampType newInstance(Instant min, Instant max) {
		return timestampType(min, max);
	}
}
