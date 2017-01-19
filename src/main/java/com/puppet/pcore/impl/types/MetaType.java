package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.impl.Constants;
import com.puppet.pcore.impl.parser.HashExpression;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Constants.KEY_ANNOTATIONS;
import static com.puppet.pcore.impl.Helpers.getArgument;
import static java.util.Collections.emptyMap;

abstract class MetaType extends AnyType implements Annotatable {
	private Map<AnyType,Map<String,?>> annotations = emptyMap();
	private Object i12nHashExpression;
	private boolean selfRecursion;

	MetaType(Expression i12nHashExpression) {
		this.i12nHashExpression = i12nHashExpression;
	}

	MetaType(Map<String,Object> unresolvedI12nHash) {
		this.i12nHashExpression = unresolvedI12nHash;
	}

	@Override
	public Map<AnyType,Map<String,?>> getAnnotations() {
		return annotations;
	}

	public boolean isResolved() {
		return i12nHashExpression == null;
	}

	public boolean isSelfRecursion() {
		return selfRecursion;
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnyType resolve() {
		if(i12nHashExpression != null) {
			selfRecursion = true;

			Map<String,Object> i12nHash;
			if(i12nHashExpression instanceof HashExpression) {
				HashExpression i12e = (HashExpression)i12nHashExpression;
				i12nHashExpression = null;
				i12nHash = resolveLiteralHash(i12e);
			} else {
				i12nHash = (Map<String,Object>)i12nHashExpression;
				i12nHashExpression = null;
				i12nHash = resolveHash(i12nHash);
			}
			initializeFromHash(i12nHash);

			RecursionGuard guard = new RecursionGuard();
			accept(NoopAcceptor.singleton, guard);
			selfRecursion = guard.recursiveThis(this);
		}
		return this;
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		getAnnotations().keySet().forEach(key -> key.accept(visitor, guard));
		super.accept(visitor, guard);
	}

	void initializeFromHash(Map<String,Object> i12nHash) {
		annotations = getArgument(KEY_ANNOTATIONS, i12nHash, emptyMap());
	}

	@SuppressWarnings("unchecked")
	Map<String,Object> resolveHash(Map<String,Object> i12nHash) {
		return (Map<String,Object>)resolveTypeRefs(i12nHash);
	}

	@SuppressWarnings("unchecked")
	Map<String,Object> resolveLiteralHash(HashExpression i12e) {
		return (Map<String,Object>)Pcore.typeEvaluator().resolve(i12e);
	}

	@SuppressWarnings("unchecked")
	Object resolveTypeRefs(Object value) {
		if(value instanceof Map<?,?>) {
			Map<Object,Object> result = new LinkedHashMap<>();
			for(Map.Entry<?,?> p : ((Map<Object,Object>)value).entrySet())
				result.put(resolveTypeRefs(p.getKey()), resolveTypeRefs(p.getValue()));
			return result;
		}

		if(value instanceof List<?>) {
			List<Object> source = (List<Object>)value;
			List<Object> result = new ArrayList<>(source.size());
			for(Object elem : source)
				result.add(resolveTypeRefs(elem));
			return result;
		}

		return value instanceof AnyType ? ((AnyType)value).resolve() : value;
	}

	public Map<String,Object> i12nHash() {
		Map<String,Object> result = new LinkedHashMap<>();
		Map<AnyType,Map<String,?>> annotations = getAnnotations();
		if(!annotations.isEmpty())
			result.put(Constants.KEY_ANNOTATIONS, annotations);
		return result;
	}

	void setI12nHashExpression(Map<String,Object> unresolvedI12nHash) {
		this.i12nHashExpression = unresolvedI12nHash;
	}
}
