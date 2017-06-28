package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.util.List;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.any;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

public class SemVerType extends ScalarType {
	static final SemVerType DEFAULT = new SemVerType(emptyList());

	private static ObjectType ptype;
	public final List<VersionRange> ranges;

	SemVerType(List<VersionRange> ranges) {
		this.ranges = ranges;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}


	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return ranges.hashCode();
	}

	@SuppressWarnings("unchecked")
	@Override
	public FactoryDispatcher<Version> factoryDispatcher() {
		AnyType formatType = stringType(2);
		return dispatcher(
				constructor(
						(args) -> Version.create((String)args.get(0)),
						stringType())
		);
	}

	@Override
	public boolean roundtripWithString() {
		return true;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::SemVerType", "Pcore::ScalarType",
				singletonMap(
						"ranges", asMap(
								KEY_TYPE, arrayType(variantType(semVerRangeType(), StringType.NOT_EMPTY)),
								KEY_VALUE, emptyList())));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, semVerTypeDispatcher(),
				(self) -> new Object[]{self.ranges});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o instanceof SemVerType && ranges.equals(((SemVerType)o).ranges);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Version) {
			Version vo = (Version)o;
			return ranges.isEmpty() || any(ranges, (range) -> range.includes(vo));
		}
		return false;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof SemVerType) {
			if(ranges.isEmpty())
				return true;

			// All ranges in st must be covered by at least one range in self
			List<VersionRange> tRanges = ((SemVerType)t).ranges;
			return !tRanges.isEmpty() && all(tRanges, oRange -> any(ranges, oRange::isAsRestrictiveAs));
		}
		return false;
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return semVerType(Helpers.mergeRanges(Helpers.mergeUnique(ranges, ((SemVerType)other).ranges)));
	}
}
