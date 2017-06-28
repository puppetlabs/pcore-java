package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Assertions;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Collections;
import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class StringType extends ScalarDataType {
	public static final StringType DEFAULT = new StringType(IntegerType.POSITIVE);
	public static final IterableType ITERABLE_TYPE = new IterableType(new StringType(integerType(1, 1)));
	public static final AnyType NOT_EMPTY = new StringType(integerType(1));

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

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public boolean equals(Object o) {
		if(o instanceof StringType) {
			StringType so = (StringType)o;
			return Objects.equals(value, so.value) && Objects.equals(size, so.size);
		}
		return false;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return Objects.hashCode(value) * 31 + Objects.hashCode(size);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(StringType.class, "Pcore::StringType", "Pcore::ScalarDataType",
				asMap(
						"size_type_or_value", asMap(
								KEY_TYPE, optionalType(variantType(StringType.DEFAULT, typeType(integerType(0)))),
								KEY_VALUE, null)),
				(args) -> stringType(args.get(0)),
				(self) -> new Object[]{self.value == null ? self.size : self.value});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		size.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		return ITERABLE_TYPE;
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
