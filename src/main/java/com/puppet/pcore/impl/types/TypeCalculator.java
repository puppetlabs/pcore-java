package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.Polymorphic;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.Helpers.reduce;
import static com.puppet.pcore.impl.types.TypeFactory.*;

@SuppressWarnings("unused")
class TypeCalculator extends Polymorphic<AnyType> {

	static final TypeCalculator SINGLETON = new TypeCalculator();
	private static final DispatchMap dispatchMap = initPolymorphicDispatch(TypeCalculator.class, "_infer");

	private TypeCalculator() {
	}

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	AnyType _infer(AnyType o) {
		return typeType(o);
	}

	AnyType _infer(Binary o) {
		return binaryType();
	}

	AnyType _infer(Boolean o) {
		return booleanType();
	}

	AnyType _infer(Byte o) {
		long val = o.longValue();
		return integerType(val, val);
	}

	AnyType _infer(Collection<?> o) {
		return collectionType(sizeAsType(o));
	}

	AnyType _infer(Class<?> o) {
		return runtimeType(((Class<?>)o).getName());
	}

	AnyType _infer(Default o) {
		return defaultType();
	}

	AnyType _infer(Duration o) {
		return timeSpanType(o, o);
	}

	AnyType _infer(Double o) {
		return floatType(o, o);
	}

	AnyType _infer(Float o) {
		double val = o.doubleValue();
		return floatType(val, val);
	}

	AnyType _infer(Instant o) {
		return timestampType(o, o);
	}

	AnyType _infer(Iterator o) {
		return iteratorType();
	}

	AnyType _infer(Integer o) {
		long val = o.longValue();
		return integerType(val, val);
	}

	AnyType _infer(List<?> o) {
		return o.isEmpty() ? ArrayType.EMPTY : arrayType(inferAndReduceType(o), sizeAsType(o));
	}

	AnyType _infer(Long o) {
		return integerType(o, o);
	}

	AnyType _infer(Map<?,?> o) {
		return o.isEmpty()
				? HashType.EMPTY
				: hashType(inferAndReduceType(o.keySet()), inferAndReduceType(o.values()), sizeAsType(o));
	}

	AnyType _infer(Object o) {
		if(o instanceof PuppetObject)
			return (AnyType)((PuppetObject)o)._pcoreType();
		return runtimeType("java", o.getClass().getName());
	}

	AnyType _infer(Pattern o) {
		return regexpType(o);
	}

	AnyType _infer(Short o) {
		long val = o.longValue();
		return integerType(val, val);
	}

	AnyType _infer(Sensitive o) {
		return sensitiveType();
	}

	AnyType _infer(String o) {
		return stringType(o);
	}

	AnyType _infer(Version o) {
		return semVerType(VersionRange.exact(o));
	}

	AnyType _infer(VersionRange o) {
		return semVerRangeType();
	}

	AnyType _infer(Void o) {
		return undefType();
	}

	AnyType infer(Object o) {
		return dispatch(o);
	}

	AnyType inferAndReduceType(Collection<?> objects) {
		return reduceType(map(objects, this::infer));
	}

	AnyType inferSet(Object o) {
		if(o instanceof Collection<?>) {
			Collection<?> cv = (Collection<?>)o;
			return cv.isEmpty() ? ArrayType.EMPTY : tupleType(map(cv, this::inferSet));
		}
		if(o instanceof Map<?,?>) {
			Map<?,?> ho = (Map<?,?>)o;
			if(all(ho.keySet(), StringType.NOT_EMPTY::isInstance))
				return structType(map(ho.entrySet(), e -> new StructElement(stringType((String)e.getKey()), inferSet(e.getValue()))));

			AnyType keyType = variantType(map(ho.keySet(), this::inferSet));
			AnyType valueType = variantType(map(ho.values(), this::inferSet));
			return hashType(keyType, valueType, sizeAsType(ho));
		}
		return infer(o);
	}

	AnyType reduceType(Collection<AnyType> types) {
		return reduce(types, unitType(), AnyType::common);
	}

	private IntegerType sizeAsType(Collection<?> c) {
		long sz = c.size();
		return integerType(sz, sz);
	}

	private IntegerType sizeAsType(Map<?,?> c) {
		long sz = c.size();
		return integerType(sz, sz);
	}
}
