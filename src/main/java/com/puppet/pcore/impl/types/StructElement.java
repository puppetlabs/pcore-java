package com.puppet.pcore.impl.types;

import com.puppet.pcore.PuppetObject;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class StructElement extends ModelObject implements PuppetObject {
	private static ObjectType ptype;
	public final AnyType key;
	public final String name;
	public final AnyType value;

	public StructElement(Object key, AnyType value) {
		if(key instanceof String) {
			this.name = (String)key;
			this.key = stringType(name);
		} else {
			this.key = (AnyType)key; // Assertions.assertType(StructType.KEY_TYPE, key, () -> "Key in StructType");
			this.name = ((StringType)this.key.actualType()).value;
		}
		this.value = value;
	}

	public static AnyType pcoreType() {
		return ptype;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public int hashCode() {
		return key.hashCode() * 31 + value.hashCode();
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::StructElement", null,
				asMap(
						"key_type", typeType(),
						"value_type", typeType()));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, structElementDispatcher(),
				(self) -> new Object[]{self.key, self.value});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o instanceof StructElement) {
			StructElement mo = (StructElement)o;
			return key.guardedEquals(mo.key, guard) && value.guardedEquals(mo.value, guard);
		}
		return false;
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		key.accept(visitor, guard);
		value.accept(visitor, guard);
	}
}
