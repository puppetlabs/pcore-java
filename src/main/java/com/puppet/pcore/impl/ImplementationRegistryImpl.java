package com.puppet.pcore.impl;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.types.RuntimeType;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ImplementationRegistryImpl implements ImplementationRegistry {

	private final Map<String,Function<?,?>> attributeProviderPerType = new HashMap<>();
	private final Map<String,FactoryDispatcher<?>> creatorPerType = new HashMap<>();
	private final List<PatternSubstitution> implNameSubstitutions = new ArrayList<>();
	private final Map<String,String> implNamesPerType = new HashMap<>();
	private final List<PatternSubstitution> typeNameSubstitutions = new ArrayList<>();
	private final Map<String,String> typeNamesPerImpl = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> Function<T,Object[]> attributeProviderFor(Type type) {
		return (Function<T,Object[]>)attributeProviderPerType.get(type.name());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryDispatcherImpl<T> creatorFor(Type type) {
		return (FactoryDispatcherImpl<T>)creatorPerType.get(type.name());
	}

	@Override
	public <T> void registerImplementation(Type type, FactoryDispatcher<T> creator, Function<T, Object[]> attributeProvider) {
		registerImplementation(type.name(), creator, attributeProvider);
	}

	@Override
	public <T> void registerImplementation(String typeName, FactoryDispatcher<T> creator, Function<T, Object[]> attributeProvider) {
		creatorPerType.put(typeName, creator);
		attributeProviderPerType.put(typeName, attributeProvider);
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
	public void registerTypeMapping(Type runtimeType, PatternSubstitution substitution) {
		if(!(runtimeType instanceof RuntimeType))
			throw new IllegalArgumentException("First argument to registerTypeMapping must be a Runtime type");
		RuntimeType rt = (RuntimeType)runtimeType;
		if(rt.pattern == null)
			throw new IllegalArgumentException("Cannot map a Runtime without pattern to a Type pattern");
		registerPatternMapping(substitution, new PatternSubstitution(rt.pattern.pattern(), rt.name));
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
