package com.puppet.pcore.impl;

import com.puppet.pcore.impl.types.ObjectType;

import java.io.IOException;

public class GivenArgumentsAccessor extends AbstractArgumentsAccessor {
	private final Object[] arguments;

	public GivenArgumentsAccessor(Object[] arguments, ObjectType type) {
		super(type, arguments.length);
		int max = parameterInfo.attributes.size();
		if(max == arguments.length)
			this.arguments = arguments;
		else {
			this.arguments = new Object[max];
			System.arraycopy(arguments, 0, this.arguments, 0, arguments.length);
		}
		assertArguments(this.arguments);
	}

	@Override
	public Object get(int index) throws IOException {
		return arguments[index];
	}

	@Override
	public Object[] getAll() throws IOException {
		return arguments;
	}

	@Override
	public <T> T remember(T createdInstance) {
		return createdInstance;
	}

	@Override
	public int size() {
		return arguments.length;
	}
}
