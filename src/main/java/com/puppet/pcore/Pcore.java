package com.puppet.pcore;

import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.types.TypeSetType;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.serialization.SerializationFactory;

import java.util.function.Supplier;

/**
 * Provides access to relevant parts of the Pcore Type system.
 */
public class Pcore {
	private static final PcoreImpl INSTANCE = new PcoreImpl();

	static {
		reset();
	}

	public static Loader loader() {
		return INSTANCE.loader();
	}

	/**
	 * For test purposes only
	 */
	public static void reset() {
		INSTANCE.initBaseTypeSystem();
	}

	public static <T> T withTypeSetScope(TypeSetType typeSetType, Supplier<T> function) {
		return INSTANCE.withTypeSetScope(typeSetType, function);
	}

	public static <T> T withLocalScope(Supplier<T> function) {
		return INSTANCE.withLocalScope(function);
	}

	public static ImplementationRegistry implementationRegistry() {
		return INSTANCE.implementationRegistry();
	}

	public static Type infer(Object value) {
		return INSTANCE.infer(value);
	}

	public static Type inferSet(Object value) {
		return INSTANCE.inferSet(value);
	}

	public static SerializationFactory serializationFactory(String serializationFormat) {
		return INSTANCE.serializationFactory(serializationFormat);
	}

	public static TypeEvaluator typeEvaluator() {
		return INSTANCE.typeEvaluator();
	}
}
