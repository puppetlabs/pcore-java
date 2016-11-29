package com.puppet.pcore.impl;

import com.puppet.pcore.ImplementationRegistry;
import com.puppet.pcore.PatternSubstitution;
import com.puppet.pcore.Type;
import com.puppet.pcore.TypeEvaluator;
import com.puppet.pcore.impl.types.RuntimeType;
import com.puppet.pcore.serialization.FactoryFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ImplementationRegistryImpl implements ImplementationRegistry {

	private final Map<String,Function<?,?>> attributeProviderPerImpl = new HashMap<>();
	private final Map<String,FactoryFunction<?>> creatorPerImpl = new HashMap<>();
	private final List<PatternSubstitution> implNameSubstitutions = new ArrayList<>();
	private final Map<String,String> implNamesPerType = new HashMap<>();
	private final List<PatternSubstitution> typeNameSubstitutions = new ArrayList<>();
	private final Map<String,String> typeNamesPerImpl = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> Function<T,Object[]> attributeProviderFor(Class<T> implClass) {
		return (Function<T,Object[]>)attributeProviderPerImpl.get(implClass.getName());
	}

	@Override
	public Class<?> classFor(Type type, ClassLoader loader) {
		return classFor(type.name(), loader);
	}

	@Override
	public Class<?> classFor(String typeName, ClassLoader loader) {
		String className = findMapping(typeName, implNamesPerType, typeNameSubstitutions);
		if(className != null)
			try {
				return loader.loadClass(className);
			} catch(ClassNotFoundException ignored) {
			}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryFunction<T> creatorFor(Class<T> implClass) {
		return (FactoryFunction<T>)creatorPerImpl.get(implClass.getName());
	}

	@Override
	public <T> void registerImplementation(
			Type type, Class<T> implClass, FactoryFunction<T> creator, Function<T,
			Object[]> attributeProvider) {
		registerImplementation(type.name(), implClass.getName(), creator, attributeProvider);
	}

	@Override
	public <T> void registerImplementation(
			String typeName, String implName, FactoryFunction<T> creator, Function<T,
			Object[]> attributeProvider) {
		typeNamesPerImpl.put(implName, typeName);
		implNamesPerType.put(typeName, implName);
		creatorPerImpl.put(implName, creator);
		attributeProviderPerImpl.put(implName, attributeProvider);
	}

	@Override
	public void registerNamespace(String typeNamespace, String implNamespace) {
		registerPatternMapping(
				new PatternSubstitution(Pattern.compile("\\A" + typeNamespace + "::(\\w+)\\z"), implNamespace + ".$1"),
				new PatternSubstitution(Pattern.compile("\\A" + implNamespace + "\\.(\\w+)\\z"), typeNamespace + "::$1"));
	}

	@Override
	public void registerPatternMapping(PatternSubstitution typeNameSubst, PatternSubstitution implNameSubst) {
		typeNameSubstitutions.add(typeNameSubst);
		implNameSubstitutions.add(implNameSubst);
	}

	@Override
	public <T> void registerTypeMapping(
			Type runtimeType, Type puppetType, FactoryFunction<T> creator, Function<T,
			Object[]> attributeSupplier) {
		if(!(runtimeType instanceof RuntimeType))
			throw new IllegalArgumentException("First argument to registerTypeMapping must be a Runtime type");
		RuntimeType rt = (RuntimeType)runtimeType;
		if(rt.pattern != null)
			throw new IllegalArgumentException("Cannot map a Runtime with pattern to a Puppet type");
		registerImplementation(puppetType.name(), rt.name, creator, attributeSupplier);
	}

	@Override
	public void registerTypeMapping(Type runtimeType, PatternSubstitution substitution) {
		if(!(runtimeType instanceof RuntimeType))
			throw new IllegalArgumentException("First argument to registerTypeMapping must be a Runtime type");
		RuntimeType rt = (RuntimeType)runtimeType;
		if(rt.pattern == null)
			throw new IllegalArgumentException("Cannot map a Runtime without pattern to a Type pattern");
		registerPatternMapping(substitution, new PatternSubstitution(rt.pattern.pattern(), rt.name));
	}

	@Override
	public Type typeFor(Class<?> implClass, TypeEvaluator evaluator) {
		return typeFor(implClass.getName(), evaluator);
	}

	@Override
	public Type typeFor(String name, TypeEvaluator evaluator) {
		String typeString = findMapping(name, typeNamesPerImpl, implNameSubstitutions);
		return typeString == null ? null : evaluator.resolveType(typeString);
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	private String findMapping(String name, Map<String,String> names, List<PatternSubstitution> substitutions) {
		synchronized(names) {
			String found = names.get(name);
			if(found != null || names.containsKey(name))
				return found;

			for(PatternSubstitution subst : substitutions) {
				String substituted = subst.replaceIn(name);
				if(substituted != null) {
					names.put(name, substituted);
					return substituted;
				}
			}
			names.put(name, null);
			return null;
		}
	}
}
