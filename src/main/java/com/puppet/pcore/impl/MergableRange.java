package com.puppet.pcore.impl;

public interface MergableRange<T> {
	boolean isOverlap(T t);

	T merge(T other);
}
