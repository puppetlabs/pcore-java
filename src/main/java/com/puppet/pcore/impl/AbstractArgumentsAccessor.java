package com.puppet.pcore.impl;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.impl.types.ObjectType.Attribute;
import com.puppet.pcore.impl.types.ObjectType.ParameterInfo;
import com.puppet.pcore.serialization.ArgumentsAccessor;

import java.util.List;

import static com.puppet.pcore.impl.TypeEvaluatorImpl.assertParameterCount;

public abstract class AbstractArgumentsAccessor implements ArgumentsAccessor {
	protected final int numberOfGivenArguments;
	protected final ParameterInfo parameterInfo;
	protected final ObjectType type;

	protected AbstractArgumentsAccessor(ObjectType type, int numberOfGivenArguments) {
		this.type = type;
		this.parameterInfo = getAndAssertParameterInfo(type, numberOfGivenArguments);
		this.numberOfGivenArguments = numberOfGivenArguments;
	}

	@Override
	public Type getType() {
		return type;
	}

	protected static ParameterInfo getAndAssertParameterInfo(ObjectType type, int numberOfArguments) {
		ParameterInfo pi = type.parameterInfo();
		int max = pi.attributes.size();
		assertParameterCount(pi.requiredCount, max, numberOfArguments, type.name());
		return pi;
	}

	protected void assertArguments(Object[] arguments) {
		List<Attribute> attributes = parameterInfo.attributes;
		int max = attributes.size();
		for(int idx = 0; idx < max; ++idx) {
			Attribute attr = attributes.get(idx);
			if(idx < numberOfGivenArguments) {
				Object value = arguments[idx];
				if(value instanceof Number) {
					// In Pcore, all numbers are 64-bit quantities
					if(value instanceof Float)
						arguments[idx] = (double)(float)value;
					else if(!(value instanceof Long || value instanceof Double))
						arguments[idx] = ((Number)value).longValue();
				}
				attr.type.assertInstanceOf(arguments[idx], attr::label);
			} else
				arguments[idx] = attr.value();
		}
	}
}
