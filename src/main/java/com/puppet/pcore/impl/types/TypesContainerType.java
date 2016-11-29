package com.puppet.pcore.impl.types;

import com.puppet.pcore.TypeEvaluator;

import java.util.ArrayList;
import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableList;

abstract class TypesContainerType extends AnyType {
	public final List<AnyType> types;
	private boolean resolved;

	TypesContainerType(List<AnyType> types, boolean resolved) {
		this.types = types;
		this.resolved = resolved;
	}

	public boolean equals(Object o) {
		return super.equals(o) && types.equals(((TypesContainerType)o).types);
	}

	@Override
	public abstract AnyType generalize();

	public int hashCode() {
		return types.hashCode();
	}

	@Override
	public synchronized AnyType resolve(TypeEvaluator evaluator) {
		if(resolved)
			return this;

		resolved = true;
		boolean changed = false;
		ArrayList<AnyType> rsTypes = new ArrayList<>(types.size());
		for(AnyType type : types) {
			AnyType rsType = type.resolve(evaluator);
			if(type != rsType)
				changed = true;
			rsTypes.add(rsType);
		}
		if(!changed)
			resolved = true;

		resolved = false;
		return copyWith(unmodifiableList(rsTypes.toArray(new AnyType[types.size()])), true);
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		types.forEach(type -> type.accept(visitor, guard));
		super.accept(visitor, guard);
	}

	abstract TypesContainerType copyWith(List<AnyType> types, boolean resolved);
}
