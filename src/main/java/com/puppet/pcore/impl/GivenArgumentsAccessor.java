package com.puppet.pcore.impl;

import com.puppet.pcore.PcoreException;
import com.puppet.pcore.impl.types.ObjectType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GivenArgumentsAccessor extends AbstractArgumentsAccessor {
	private final Object[] arguments;

	public GivenArgumentsAccessor(ObjectType type, Object...arguments) {
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
	public Object get(int index) {
		return arguments[index];
	}

	@Override
	public Object[] getAll() {
		return arguments;
	}

	@Override
	public List<Object> getArgumentList() {
		return Arrays.asList(getAll());
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
