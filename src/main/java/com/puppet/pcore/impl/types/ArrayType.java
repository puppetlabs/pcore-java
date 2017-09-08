package com.puppet.pcore.impl.types;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Type;
import com.puppet.pcore.TypeConversionException;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.util.*;

import static com.puppet.pcore.impl.Assertions.assertNotNull;
import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class ArrayType extends CollectionType {
	static final ArrayType DEFAULT = new ArrayType(AnyType.DEFAULT, IntegerType.POSITIVE);
	static final ArrayType EMPTY = new ArrayType(UnitType.DEFAULT, IntegerType.ZERO_SIZE);

	private static ObjectType ptype;

	ArrayType(AnyType elementType, IntegerType size) {
		this(assertNotNull(elementType, () -> "Array element type"), assertNotNull(size, () -> "Array attributeCount type"), false);
	}

	ArrayType(AnyType elementType, IntegerType size, boolean resolved) {
		super(elementType, size, resolved);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new ArrayType(type.generalize(), IntegerType.POSITIVE);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::ArrayType", "Pcore::CollectionType",
				singletonMap("element_type", asMap(
						KEY_TYPE, typeType(),
						KEY_VALUE, anyType())),
				asList("element_type", "size_type"));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, arrayTypeDispatcher(), (self) -> new Object[]{self.type, self.size});
	}

	@Override
	public FactoryDispatcher<List<?>> factoryDispatcher() {
		return arrayFactoryDispatcher();
	}

	@SuppressWarnings("unchecked")
	static FactoryDispatcher<List<?>> arrayFactoryDispatcher() {
		return dispatcher(
			constructor((args) -> {
				Object value = args.get(0);
				boolean wrap = args.size() == 2 && (Boolean)args.get(1);
				if(wrap)
					return singletonList(value);
				if(value instanceof List<?>)
					return (List<Object>)value;
				if(value instanceof Map<?,?>)
					return mapAsPairs((Map<?,?>)value);
				if(value instanceof Collection<?>)
					return new ArrayList<>((Collection<?>)value);
				if(value instanceof String) {
					String str = (String)value;
					int idx = str.length();
					String[] chars = new String[idx];
					while(--idx >= 0)
						chars[idx] = str.substring(idx, idx+1);
					return asList(chars);
				}
				if(value instanceof Binary)
					return ((Binary)value).asList();
				if(value instanceof Iterable<?>) {
					ArrayList<Object> result = new ArrayList<>();
					Iterable<?> elems = (Iterable)value;
					for(Object elem : elems)
						result.add(elem);
					return result;
				}
				if(value instanceof Iterator<?>) {
					ArrayList<Object> result = new ArrayList<>();
					Iterator<?> iter = (Iterator)value;
					while(iter.hasNext())
						result.add(iter.next());
					return result;
				}
				throw new TypeConversionException(
						format("Value of type '%s' cannot be converted to an Array", infer(value).generalize()));
				},
				tupleType(asList(anyType(), booleanType()), 1, 2))
		);
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new ArrayType(type, size, resolved);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof List<?>) {
			List<?> lo = (List<?>)o;
			return size.isInstance(lo.size(), guard) && (type.equals(AnyType.DEFAULT) || all(lo, (v) -> type.isInstance(v, guard)));
		}
		return false;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof TupleType)
			return super.isUnsafeAssignable(t, guard) && all(((TupleType)t).types, tt -> type.isAssignable(tt, guard));

		return t instanceof ArrayType && super.isUnsafeAssignable(t, guard) && type.isAssignable(
				((ArrayType)t).type,
				guard);
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		return arrayType(type.common(((ArrayType)other).type), (IntegerType)size.common(((ArrayType)other).size));
	}
}
