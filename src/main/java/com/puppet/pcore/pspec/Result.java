package com.puppet.pcore.pspec;

public interface Result<T> {
	Executable createTest(T actual);
}
