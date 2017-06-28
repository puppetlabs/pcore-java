package com.puppet.pcore.impl;

import com.puppet.pcore.Type;
import com.puppet.pcore.serialization.ArgumentsAccessor;

import java.io.IOException;
import java.util.List;

public abstract class SelfReferencingFactoryImpl<C> extends FactoryDispatcherImpl<C> {
	public SelfReferencingFactoryImpl(List<ConstructorImpl<C>> constructors) {
		super(constructors);
	}

	@Override
	public abstract C createInstance(Type type, ArgumentsAccessor aa) throws IOException;
}
