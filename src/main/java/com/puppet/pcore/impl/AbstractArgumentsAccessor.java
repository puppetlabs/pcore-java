package com.puppet.pcore.impl;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.types.ObjectType;
import com.puppet.pcore.impl.types.ObjectType.Attribute;
import com.puppet.pcore.impl.types.ParameterInfo;
import com.puppet.pcore.impl.types.TupleType;
import com.puppet.pcore.serialization.ArgumentsAccessor;

import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.TypeEvaluatorImpl.assertParameterCount;
import static com.puppet.pcore.impl.types.TypeFactory.tupleType;

public abstract class AbstractArgumentsAccessor implements ArgumentsAccessor {
	protected final int numberOfGivenArguments;
	protected final ParameterInfo parameterInfo;
	protected final ObjectType type;

	protected AbstractArgumentsAccessor(ObjectType type, int numberOfGivenArguments) {
		this.type = type;
		this.parameterInfo = getAndAssertParameterInfo(type, numberOfGivenArguments);
		List<Attribute> attrs = parameterInfo.attributes;
		this.numberOfGivenArguments = numberOfGivenArguments;
	}

	@Override
	public ObjectType getType() {
		return type;
	}

	public TupleType getParametersType() {
		return parameterInfo.parametersType();
	}

	protected static ParameterInfo getAndAssertParameterInfo(ObjectType type, int numberOfArguments) {
		ParameterInfo pi = type.parameterInfo();
		int max = pi.attributes.size();
		if(!(numberOfArguments == 1 && type.roundtripWithHash()))
			assertParameterCount(pi.requiredCount, max, numberOfArguments, type.name());
		return pi;
	}

	protected void assertArguments(Object[] arguments) {
		if(arguments.length == 1 && arguments[0] instanceof Map && type.roundtripWithHash()) {
			type.assertHashInitializer((Map<?,?>)arguments[0]);
			return;
		}

		List<Attribute> attributes = parameterInfo.attributes;
		int max = attributes.size();
		if(max > arguments.length)
			max = arguments.length;

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
