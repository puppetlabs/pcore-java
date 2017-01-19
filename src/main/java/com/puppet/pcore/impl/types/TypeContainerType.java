package com.puppet.pcore.impl.types;

import java.util.Objects;

abstract class TypeContainerType extends AnyType {
	public final AnyType type;

	private boolean resolved;

	TypeContainerType(AnyType type, boolean resolved) {
		this.type = type;
		this.resolved = resolved;
	}

	public boolean equals(Object o) {
		return super.equals(o) && Objects.equals(type, ((TypeContainerType)o).type);
	}

	@Override
	public abstract AnyType generalize();

	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(type);
	}

	@Override
	public synchronized AnyType resolve() {
		if(type == null || resolved)
			return this;
		AnyType resolvedType = type.resolve();
		if(resolvedType == type) {
			resolved = true;
			return this;
		}
		return copyWith(resolvedType, true);
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		type.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	abstract AnyType copyWith(AnyType type, boolean resolved);
}
