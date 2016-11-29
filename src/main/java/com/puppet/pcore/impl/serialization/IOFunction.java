package com.puppet.pcore.impl.serialization;

import java.io.IOException;

@FunctionalInterface
public interface IOFunction<T, R> {
	R apply(T t) throws IOException;
}
