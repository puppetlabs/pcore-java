package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.impl.AbstractArgumentsAccessor;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.serialization.Deserializer;

import java.io.IOException;

import static com.puppet.pcore.impl.Constants.EMPTY_ARRAY;

class DeserializerArgumentsAccessor extends AbstractArgumentsAccessor {
	private enum State {Unknown, Complete, ReplaceAfter}

	private final Object[] arguments;
	private final DeserializerImpl deserializer;
	private boolean initialized;
	private State remembered;

	DeserializerArgumentsAccessor(Deserializer deserializer, ObjectType type, int numberOfArguments) {
		super(type, numberOfArguments);
		this.deserializer = (DeserializerImpl)deserializer;

		int max = parameterInfo.attributes.size();
		this.arguments = max > 0 ? new Object[max] : EMPTY_ARRAY;
		this.initialized = max == 0;
		this.remembered = State.Unknown;
	}

	@Override
	public Object get(int index) throws IOException {
		if(!initialized)
			initialize();
		return arguments[index];
	}

	@Override
	public Object[] getAll() throws IOException {
		if(!initialized)
			initialize();
		return arguments;
	}

	@Override
	public <T> T remember(T createdInstance) {
		switch(remembered) {
		case Unknown:
			deserializer.remember(createdInstance);
			remembered = State.Complete;
			break;
		case ReplaceAfter:
			deserializer.replacePlaceHolder(this, createdInstance);
			remembered = State.Complete;
			break;
		}
		return createdInstance;
	}

	@Override
	public int size() {
		return arguments.length;
	}

	private void initialize() throws IOException {
		if(remembered == State.Complete) {
			for(int idx = 0; idx < numberOfGivenArguments; ++idx)
				arguments[idx] = deserializer.read();
		} else {
			deserializer.remember(this);
			remembered = State.ReplaceAfter;
			for(int idx = 0; idx < numberOfGivenArguments; ++idx)
				arguments[idx] = deserializer.read();
		}
		assertArguments(arguments);
		initialized = true;
	}
}
