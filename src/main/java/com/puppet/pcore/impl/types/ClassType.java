package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class ClassType extends CatalogEntryType {
	public static final ClassType DEFAULT = new ClassType(null);

	private static ObjectType ptype;
	public final String className;

	ClassType(String className) {
		this.className = className;
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
		return super.hashCode() * 31 + Objects.hashCode(className);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::ClassType", "Pcore::CatalogEntryType", asMap(
				"class_name", asMap(
						KEY_TYPE, optionalType(stringType()),
						KEY_VALUE, null)));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, classTypeDispatcher(),
				(self) -> new Object[]{self.className});
	}

	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && Objects.equals(className, ((ClassType)o).className);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof ClassType && (className == null || className.equals(((ClassType)t).className));
	}
}
