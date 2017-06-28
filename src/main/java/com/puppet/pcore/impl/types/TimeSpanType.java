package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.time.Duration;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.timeSpanType;

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

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(TimeSpanType.class, "Pcore::TimeSpanType", "Pcore::ScalarType",
				asMap(
						"min", asMap(
								KEY_TYPE, timeSpanType(),
								KEY_VALUE, MIN_DURATION),
						"max", asMap(
								KEY_TYPE, timeSpanType(),
								KEY_VALUE, MAX_DURATION)),
				(args) -> timeSpanType((Duration)args.get(0), (Duration)args.get(1)),
				(self) -> new Object[]{self.min, self.max});
	}

	@Override
	TimeSpanType newInstance(Duration min, Duration max) {
		return timeSpanType(min, max);
	}
}
