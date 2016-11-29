package com.puppet.pcore.impl.types;

import com.puppet.pcore.impl.MergableRange;

abstract class TimeDataType<T extends TimeDataType, E extends Comparable<E>> extends ScalarType implements
		MergableRange<T> {
	public final E max;
	public final E min;

	TimeDataType(E min, E max) {
		this.min = min;
		this.max = max;
	}

	public boolean equals(Object o) {
		if(getClass().isInstance(o)) {
			@SuppressWarnings("unchecked") TimeDataType<T,E> ft = (T)o;
			return min.equals(ft.min) && max.equals(ft.max);
		}
		return false;
	}

	public int hashCode() {
		return (super.hashCode() * 31 + min.hashCode()) * 31 + max.hashCode();
	}

	@Override
	public boolean isOverlap(T o) {
		@SuppressWarnings("unchecked") TimeDataType<T,E> ft = o;
		return !(max.compareTo(ft.min) < 0 || ft.max.compareTo(min) < 0);
	}

	@Override
	public T merge(T o) {
		@SuppressWarnings("unchecked") TimeDataType<T,E> ft = o;
		return isOverlap(o)
				? newInstance(min.compareTo(ft.min) <= 0 ? min : ft.min, max.compareTo(ft.max) >= 0 ? max : ft.max)
				: null;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(getClass().isInstance(t)) {
			@SuppressWarnings("unchecked") TimeDataType<T,E> ft = (T)t;
			return min.compareTo(ft.min) <= 0 && max.compareTo(ft.max) >= 0;
		}
		return false;
	}

	abstract T newInstance(E min, E max);

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		@SuppressWarnings("unchecked") TimeDataType<T,E> it = (T)other;
		return newInstance(min.compareTo(it.min) <= 0 ? min : it.min, max.compareTo(it.max) >= 0 ? max : it.max);
	}
}
