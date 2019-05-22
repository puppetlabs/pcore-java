package com.puppet.pcore.impl.types;

import com.puppet.pcore.Sensitive;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class SensitiveType extends TypeContainerType {
	static final SensitiveType DEFAULT = new SensitiveType(AnyType.DEFAULT);

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

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryDispatcher<T> factoryDispatcher() {
		return (FactoryDispatcher<T>)dispatcher(
				constructor(
						(args) -> new Sensitive(args.get(0)),
						anyType())
		);
	}

	@Override
	protected AnyType copyWith(AnyType type, boolean resolved) {
		return new SensitiveType(type, resolved);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::SensitiveType", "Pcore::AnyType",
				asMap(
						"type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType())));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, sensitiveTypeDispatcher(),
				(self) -> new Object[]{self.type});
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Sensitive && type.isInstance(((Sensitive)o).unwrap(), guard);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof SensitiveType && type.isAssignable(((SensitiveType)t).type, guard);
	}
}
