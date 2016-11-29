package com.puppet.pcore.impl.serialization;

import java.io.IOException;

@FunctionalInterface
public interface IOConsumer<T> {
	void accept(T t) throws IOException;
}
