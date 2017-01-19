package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Objects;

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
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		if(o instanceof CallableType) {
			CallableType ct = (CallableType)o;
			return parametersType.equals(ct.parametersType) && Objects.equals(blockType, ct.blockType) && Objects.equals
					(returnType, ct.returnType);
		}
		return false;
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

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(CallableType.class, "Pcore::CallableType", "Pcore::AnyType", asMap(
				"param_types", asMap(
						KEY_TYPE, typeType(tupleType()),
						KEY_VALUE, tupleType()),
				"block_type", asMap(
						KEY_TYPE, optionalType(typeType(callableType())),
						KEY_VALUE, null),
				"return_type", asMap(
						KEY_TYPE, optionalType(typeType()),
						KEY_VALUE, anyType())),
				(attrs) -> callableType((TupleType)attrs.get(0), (CallableType)attrs.get(1), (AnyType)attrs.get(2)),
				(self) -> new Object[]{self.parametersType, self.blockType, self.returnType}
		);
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
