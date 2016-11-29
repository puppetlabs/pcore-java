package com.puppet.pcore;

import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.SerializationFactory;

/**
 * Provides access to relevant parts of the Pcore Type system.
 */
public interface Pcore {
	Pcore INSTANCE = new PcoreImpl();

	ImplementationRegistry implementationRegistry();

	Type infer(Object value);

	Type inferSet(Object value);

	SerializationFactory serializationFactory(String serializationFormat);

	TypeEvaluator typeEvaluator();
}
