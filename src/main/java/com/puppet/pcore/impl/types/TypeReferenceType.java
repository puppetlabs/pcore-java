package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.typeReferenceTypeDispatcher;

public class TypeReferenceType extends AnyType {
	static final TypeReferenceType DEFAULT = new TypeReferenceType("UnresolvedReference");

	private static ObjectType ptype;
	public final String typeString;

	TypeReferenceType(String typeString) {
		this.typeString = typeString;
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
		return typeString.hashCode();
	}

	@Override
	public AnyType resolve() {
		return typeString.equals(DEFAULT.typeString) ? this : (AnyType)Pcore.typeEvaluator().resolveType(typeString);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::TypeReferenceType", "Pcore::AnyType",
				asMap(
						"typeString", StringType.NOT_EMPTY));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, typeReferenceTypeDispatcher(),
				(self) -> new Object[]{self.typeString});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o instanceof TypeReferenceType && typeString.equals(((TypeReferenceType)o).typeString);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return false;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return equals(t);
	}
}
