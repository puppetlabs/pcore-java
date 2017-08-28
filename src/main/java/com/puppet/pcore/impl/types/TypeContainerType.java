package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;

import java.util.Objects;

abstract class TypeContainerType extends AnyType {
	public final AnyType type;

	private boolean resolved;

	TypeContainerType(AnyType type, boolean resolved) {
		this.type = type;
		this.resolved = resolved;
	}

	@Override
	public abstract AnyType generalize();

	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(type);
	}

	@Override
	public synchronized AnyType resolve(Pcore pcore) {
		if(type == null || resolved)
			return this;
		AnyType resolvedType = type.resolve(pcore);
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

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && equals(type, ((TypeContainerType)o).type, guard);
	}
}
