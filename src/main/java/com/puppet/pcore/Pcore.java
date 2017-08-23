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
	private static final ThreadLocal<PcoreImpl> INSTANCE = new ThreadLocal<>();

	private static PcoreImpl getPcoreImpl() {
		PcoreImpl instance = INSTANCE.get();
		if(instance == null) {
			instance = new PcoreImpl();
			INSTANCE.set(instance);
			instance.initBaseTypeSystem();
		}
		return instance;
	}

	public static Loader loader() {
		return getPcoreImpl().loader();
	}

	/**
	 * For test purposes only
	 */
	public static void reset() {
		getPcoreImpl().initBaseTypeSystem();
	}

	public static <T> T withTypeSetScope(TypeSetType typeSetType, Supplier<T> function) {
		return getPcoreImpl().withTypeSetScope(typeSetType, function);
	}

	public static <T> T withLocalScope(Supplier<T> function) {
		return getPcoreImpl().withLocalScope(function);
	}

	public static ImplementationRegistry implementationRegistry() {
		return getPcoreImpl().implementationRegistry();
	}

	public static Type infer(Object value) {
		return getPcoreImpl().infer(value);
	}

	public static Type inferSet(Object value) {
		return getPcoreImpl().inferSet(value);
	}

	public static SerializationFactory serializationFactory(String serializationFormat) {
		return getPcoreImpl().serializationFactory(serializationFormat);
	}

	public static TypeEvaluator typeEvaluator() {
		return getPcoreImpl().typeEvaluator();
	}
}
