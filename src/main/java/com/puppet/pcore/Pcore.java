package com.puppet.pcore;

import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.loader.ParentedLoader;
import com.puppet.pcore.impl.loader.TypeSetLoader;
import com.puppet.pcore.impl.types.TypeSetType;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.serialization.SerializationFactory;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides access to relevant parts of the Pcore Type system.
 */
public abstract class Pcore {
	public static Pcore create() {
		return new PcoreImpl(new ParentedLoader(staticPcore().loader()));
	}

	public static Pcore staticPcore() {
		return PcoreImpl.staticInstance();
	}

	/**
	 * Prevent further modifications to this pcore instance
	 */
	public abstract void freeze();

	public abstract Loader loader();

	public abstract ImplementationRegistry implementationRegistry();

	public abstract Type infer(Object value);

	public abstract Type inferSet(Object value);

	public abstract SerializationFactory serializationFactory(String serializationFormat);

	public abstract TypeEvaluator typeEvaluator();

	public abstract Pcore withLocalScope();

	public abstract Pcore withTypeSetScope(TypeSetType typeSet);
}
