package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.FactoryDispatcher;

import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.types.TypeFactory.semVerRangeTypeDispatcher;
import static com.puppet.pcore.impl.types.TypeFactory.stringType;

public class SemVerRangeType extends AnyType {
	static final SemVerRangeType DEFAULT = new SemVerRangeType();

	private static ObjectType ptype;

	private SemVerRangeType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@SuppressWarnings("unchecked")
	@Override
	public FactoryDispatcher<VersionRange> factoryDispatcher() {
		AnyType formatType = stringType(2);
		return dispatcher(
				constructor(
						(args) -> VersionRange.create((String)args.get(0)),
						stringType())
		);
	}

	@Override
	public boolean roundtripWithString() {
		return true;
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof VersionRange;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof SemVerRangeType;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::SemVerRangeType", "Pcore::AnyType");
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, semVerRangeTypeDispatcher());
	}
}
