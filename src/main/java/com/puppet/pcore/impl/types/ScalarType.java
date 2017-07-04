package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.semver.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.types.TypeFactory.scalarTypeDispatcher;

public class ScalarType extends AnyType {
	public static final ScalarType DEFAULT = new ScalarType();

	private static ObjectType ptype;

	ScalarType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::ScalarType", "Pcore::AnyType");
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, scalarTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o == null
				|| o instanceof String
				|| o instanceof Number
				|| o instanceof Boolean
				|| o instanceof Pattern
				|| o instanceof Instant
				|| o instanceof Duration
				|| o instanceof Version;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof ScalarType;
	}
}
