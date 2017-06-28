package com.puppet.pcore.impl;

import com.puppet.pcore.Type;
import com.puppet.pcore.TypeAssertionException;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.io.IOException;
import java.util.List;

import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.types.TypeFactory.inferSet;
import static com.puppet.pcore.impl.types.TypeFactory.variantType;
import static java.lang.String.format;

public class FactoryDispatcherImpl<C> implements FactoryDispatcher<C> {
	private final List<ConstructorImpl<C>> constructors;

	@SafeVarargs
	public static <C> FactoryDispatcherImpl<C> dispatcher(ConstructorImpl<C> ...constructors) {
		return new FactoryDispatcherImpl<>(asList(constructors));
	}

	public FactoryDispatcherImpl(List<ConstructorImpl<C>> constructors) {
		this.constructors = constructors;
	}

	@Override
	public List<? extends ConstructorImpl<C>> constructors() {
		return constructors;
	}

	@Override
	public C createInstance(Type type, ArgumentsAccessor aa) throws IOException {
		return aa.remember(createInstance(type, aa.getArgumentList()));
	}

	@Override
	public C createInstance(Type type, Object...args) {
		return createInstance(type, asList(args));
	}

	@Override
	public C createInstance(Type type, List<Object> args) {
		for(ConstructorImpl<C> ctor : constructors)
			if(ctor.signature().isInstance(args))
				return ctor.initFunction().createInstance(args);

		throw new TypeAssertionException(format(
				"The factory that creates instances of type '%s' %s",
				type,
				TypeMismatchDescriber.SINGLETON.describeMismatch(variantType(map(constructors, ConstructorImpl::signature)), inferSet(args))));
	}
}
