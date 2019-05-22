package com.puppet.pcore.impl.types;

import com.puppet.pcore.Default;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Assertions;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.StringConverter;
import com.puppet.pcore.regex.Regexp;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.util.Collections;
import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class StringType extends ScalarDataType {
	static final StringType DEFAULT = new StringType(integerType(0));
	static final IterableType ITERABLE_TYPE = new IterableType(new StringType(integerType(1, 1)));
	static final AnyType NOT_EMPTY = new StringType(integerType(1));

	public static final Regexp FORMAT_PATTERN = Regexp.compile("^%([\\s\\[+#0{<(|-]*)([1-9][0-9]*)?(?:\\.([0-9]+))?([a-zA-Z])$");
	private static ObjectType ptype;
	public final IntegerType size;
	public final String value;

	StringType(String value) {
		this.value = value;
		int sz = value.length();
		this.size = integerType(sz, sz);
	}

	StringType(IntegerType size) {
		this.value = null;
		this.size = Assertions.assertPositive(size, () -> "String attributeCount");
	}

	public static String fromArgs(Object value, Object formats) {
		return StringConverter.singleton.convert(value, formats);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryDispatcher<T> factoryDispatcher() {
		AnyType formatType = patternType(regexpType(FORMAT_PATTERN));
		AnyType containerFormatType = structType(
				structElement(optionalType("format"), formatType),
				structElement(optionalType("separator"), stringType()),
				structElement(optionalType("separator2"), stringType()),
				structElement(optionalType("string_formats"), hashType(typeType(), formatType))
		);
		AnyType typeMapType = hashType(typeType(), variantType(formatType, containerFormatType));

		return (FactoryDispatcher<T>)dispatcher(
			constructor((args) -> fromArgs(args.get(0), Default.SINGLETON),
					anyType()),
			constructor((args) -> fromArgs(args.get(0), args.get(1)),
					anyType(), variantType(defaultType(), stringType(integerType(1)), typeMapType))
		);
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return Objects.hashCode(value) * 31 + Objects.hashCode(size);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::StringType", "Pcore::ScalarDataType",
				asMap(
						"size_type_or_value", asMap(
								KEY_TYPE, optionalType(variantType(StringType.DEFAULT, typeType(integerType(0)))),
								KEY_VALUE, null)));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, stringTypeDispatcher(),
				(self) -> new Object[]{self.value == null ? self.size : self.value});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		size.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof String
				&& size.isInstance(((String)o).length())
				&& (value == null || value.equals(o));
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		return ITERABLE_TYPE;
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o instanceof StringType) {
			StringType so = (StringType)o;
			return Objects.equals(value, so.value) && Objects.equals(size, so.size);
		}
		return false;
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof StringType) {
			if(value == null)
				return size.isAssignable(((StringType)t).size);
			return value.equals(((StringType)t).value);
		}

		if(t instanceof EnumType) {
			EnumType et = (EnumType)t;
			if(value == null)
				return size.equals(IntegerType.POSITIVE) || all(et.enums, e -> size.isInstance(e.length()));
			return et.enums.size() == 1 && value.equals(et.enums.get(0));
		}

		if(t instanceof PatternType) {
			if(value != null)
				// It's impossible to assert that no other values value matches the pattern
				return false;
			// true if size constraint is at least 0 to +Infinity
			return size.isAssignable(integerTypePositive());
		}
		return false;
	}

	@Override
	AnyType notAssignableCommon(AnyType other) {
		if(other instanceof EnumType) {
			return value != null
					? enumType(Helpers.mergeUnique(((EnumType)other).enums, Collections.singletonList(value)))
					: stringType();
		}
		return super.notAssignableCommon(other);
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		StringType st = (StringType)other;
		return value == null || st.value == null
				? stringType((IntegerType)size.common(st.size))
				: enumType(value, st.value);
	}
}
