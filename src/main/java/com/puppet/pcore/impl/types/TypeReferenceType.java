package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Helpers.asMap;

public class TypeReferenceType extends AnyType {
	public static final TypeReferenceType DEFAULT = new TypeReferenceType("UnresolvedReference");

	private static ObjectType ptype;
	public final String typeString;

	TypeReferenceType(String typeString) {
		this.typeString = typeString;
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		return o instanceof TypeReferenceType && typeString.equals(((TypeReferenceType)o).typeString);
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
		return ptype = pcore.createObjectType(TypeReferenceType.class, "Pcore::TypeReferenceType", "Pcore::AnyType",
				asMap(
						"typeString", StringType.NOT_EMPTY),
				(args) -> new TypeReferenceType((String)args.get(0)),
				(self) -> new Object[]{self.typeString});
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return equals(t);
	}
}
