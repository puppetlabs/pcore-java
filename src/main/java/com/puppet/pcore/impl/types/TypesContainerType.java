package com.puppet.pcore.impl.types;

import java.util.ArrayList;
import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

abstract class TypesContainerType extends AnyType {
	public final List<AnyType> types;
	private boolean resolved;

	TypesContainerType(List<AnyType> types, boolean resolved) {
		this.types = types;
		this.resolved = resolved;
	}

	@Override
	public abstract AnyType generalize();

	public int hashCode() {
		return types.hashCode();
	}

	@Override
	public synchronized AnyType resolve() {
		if(resolved)
			return this;

		resolved = true;
		boolean changed = false;
		ArrayList<AnyType> rsTypes = new ArrayList<>(types.size());
		for(AnyType type : types) {
			AnyType rsType = type.resolve();
			if(type != rsType)
				changed = true;
			rsTypes.add(rsType);
		}
		if(!changed)
			resolved = true;

		resolved = false;
		return copyWith(unmodifiableCopy(rsTypes), true);
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		for(AnyType type : types)
			type.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	abstract TypesContainerType copyWith(List<AnyType> types, boolean resolved);

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && equals(types, ((TypesContainerType)o).types, guard);
	}
}
