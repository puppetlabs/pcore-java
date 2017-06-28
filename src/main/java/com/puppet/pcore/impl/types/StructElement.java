package com.puppet.pcore.impl.types;

import com.puppet.pcore.PObject;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Assertions;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class StructElement extends ModelObject implements PObject {
	private static ObjectType ptype;
	public final AnyType key;
	public final String name;
	public final AnyType value;

	public StructElement(Object key, AnyType value) {
		if(key instanceof String) {
			this.name = (String)key;
			this.key = stringType(name);
		} else {
			this.key = Assertions.assertType(StructType.KEY_TYPE, key, () -> "Key in StructType");
			this.name = ((StringType)this.key.actualType()).value;
		}
		this.value = value;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public boolean equals(Object o) {
		if(o instanceof StructElement) {
			StructElement mo = (StructElement)o;
			return key.equals(mo.key) && value.equals(mo.value);
		}
		return false;
	}

	public int hashCode() {
		return key.hashCode() * 31 + value.hashCode();
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(StructElement.class, "Pcore::StructElement", null,
				asMap(
						"key_type", typeType(),
						"value_type", typeType()),
				(args) -> structElement((AnyType)args.get(0), (AnyType)args.get(1)),
				(self) -> new Object[]{self.key, self.value});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		key.accept(visitor, guard);
		value.accept(visitor, guard);
	}
}
