package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Assertions.assertNotNull;
import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class CallableType extends AnyType {
	public static final CallableType DEFAULT = new CallableType(TupleType.DEFAULT, null, AnyType.DEFAULT);
	public static final CallableType ALL = new CallableType(TupleType.EXPLICIT_EMPTY, null, AnyType.DEFAULT);

	private static ObjectType ptype;
	public final CallableType blockType;
	public final TupleType parametersType;
	public final AnyType returnType;
	private boolean resolved;

	CallableType(TupleType parametersType, CallableType blockType, AnyType returnType) {
		this(assertNotNull(parametersType, () -> "Callable parameters type"),
				blockType, assertNotNull(returnType, () -> "Callable return type"), false);
	}

	private CallableType(TupleType parametersType, CallableType blockType, AnyType returnType, boolean resolved) {
		this.parametersType = parametersType;
		this.blockType = blockType;
		this.returnType = returnType;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return ALL;
	}

	public int hashCode() {
		int hashCode = parametersType.hashCode();
		if(blockType != null)
			hashCode = hashCode * 31 + blockType.hashCode();
		if(returnType != null)
			hashCode = hashCode * 31 + returnType.hashCode();
		return hashCode;
	}

	@Override
	public synchronized AnyType resolve() {
		if(resolved)
			return this;

		CallableType rsBlockType = blockType == null ? null : (CallableType)blockType.resolve();
		TupleType rsParametersType = (TupleType)parametersType.resolve();
		AnyType rsReturnType = returnType == null ? null : returnType.resolve();
		if(blockType == rsBlockType && parametersType == rsParametersType && returnType == rsReturnType) {
			resolved = true;
			return this;
		}
		return new CallableType(rsParametersType, rsBlockType, rsReturnType, true);
	}

	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o instanceof CallableType) {
			CallableType ct = (CallableType)o;
			return equals(parametersType, ct.parametersType, guard) && equals(blockType, ct.blockType, guard) && equals(returnType, ct.returnType, guard);
		}
		return false;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::CallableType", "Pcore::AnyType", asMap(
				"param_types", asMap(
						KEY_TYPE, typeType(tupleType()),
						KEY_VALUE, tupleType()),
				"block_type", asMap(
						KEY_TYPE, optionalType(typeType(callableType())),
						KEY_VALUE, null),
				"return_type", asMap(
						KEY_TYPE, optionalType(typeType()),
						KEY_VALUE, anyType())));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, callableTypeDispatcher(),
				(self) -> new Object[]{self.parametersType, self.blockType, self.returnType}
		);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return isAssignable(infer(o), guard);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(!(t instanceof CallableType))
			return false;
		CallableType ct = (CallableType)t;
		if(!(returnType == null || returnType.isAssignable(ct.returnType, guard)))
			return false;

		// default parametersType and compatible return type means other Callable is assignable
		if(parametersType.equals(TupleType.DEFAULT))
			return true;

		// NOTE: these tests are made in reverse as it is calling the callable that is constrained
		// (it's lower bound), not its upper bound
		return ct.parametersType.isAssignable(parametersType, guard) && (ct.blockType == null || ct.blockType.isAssignable
				(blockType, guard));

	}
}
