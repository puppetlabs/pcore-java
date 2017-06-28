package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class SensitiveType extends TypeContainerType {
	public static final SensitiveType DEFAULT = new SensitiveType(AnyType.DEFAULT);

	private static ObjectType ptype;

	SensitiveType(AnyType type) {
		this(type, false);
	}

	private SensitiveType(AnyType type, boolean resolved) {
		super(type, false);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@Override
	protected AnyType copyWith(AnyType type, boolean resolved) {
		return new SensitiveType(type, resolved);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(SensitiveType.class, "Pcore::SensitiveType", "Pcore::AnyType",
				asMap(
						"type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())),
				(args) -> sensitiveType((AnyType)args.get(0)),
				(self) -> new Object[]{self.type});
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof SensitiveType && type.isAssignable(((SensitiveType)t).type, guard);
	}
}
