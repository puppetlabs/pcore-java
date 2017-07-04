package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.impl.Constants;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.parser.HashExpression;
import com.puppet.pcore.parser.Expression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Constants.KEY_ANNOTATIONS;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Helpers.getArgument;
import static java.util.Collections.emptyMap;

abstract class MetaType extends AnyType implements Annotatable {
	private Map<AnyType,Map<String,?>> annotations = emptyMap();
	private Object initHashExpression;
	private boolean selfRecursion;

	MetaType(Expression initHashExpression) {
		this.initHashExpression = initHashExpression;
	}

	MetaType(Map<String,Object> unresolvedInitHash) {
		this.initHashExpression = unresolvedInitHash;
	}

	@Override
	public Map<AnyType,Map<String,?>> getAnnotations() {
		return annotations;
	}

	public boolean isResolved() {
		return initHashExpression == null;
	}

	public boolean isSelfRecursion() {
		return selfRecursion;
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnyType resolve() {
		if(initHashExpression != null) {
			selfRecursion = true;

			Map<String,Object> initHash;
			if(initHashExpression instanceof HashExpression) {
				HashExpression i12e = (HashExpression)initHashExpression;
				initHashExpression = null;
				initHash = resolveLiteralHash(i12e);
			} else {
				initHash = (Map<String,Object>)initHashExpression;
				initHashExpression = null;
				initHash = resolveHash(initHash);
			}
			initializeFromHash(initHash);

			RecursionGuard guard = new RecursionGuard();
			accept(NoopAcceptor.singleton, guard);
			selfRecursion = guard.recursiveThis(this);
		}
		return this;
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		for(AnyType key : getAnnotations().keySet())
			key.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	void initializeFromHash(Map<String,Object> initHash) {
		annotations = getArgument(KEY_ANNOTATIONS, initHash, emptyMap());
	}

	@SuppressWarnings("unchecked")
	Map<String,Object> resolveHash(Map<String,Object> initHash) {
		return (Map<String,Object>)resolveTypeRefs(initHash);
	}

	@SuppressWarnings("unchecked")
	Map<String,Object> resolveLiteralHash(HashExpression i12e) {
		return (Map<String,Object>)Pcore.typeEvaluator().resolve(i12e);
	}

	@SuppressWarnings("unchecked")
	Object resolveTypeRefs(Object value) {
		if(value instanceof Map<?,?>) {
			Map<?,?> map = (Map<?,?>)value;
			Object[] keyValuePairs = new Object[map.size() * 2];
			int idx = 0;
			for(Map.Entry<?,?> p : ((Map<Object,Object>)value).entrySet()) {
				keyValuePairs[idx++] = resolveTypeRefs(p.getKey());
				keyValuePairs[idx++] = resolveTypeRefs(p.getValue());
			}
			return asMap(keyValuePairs);
		}

		if(value instanceof List<?>) {
			List<Object> source = (List<Object>)value;
			Object[] result = new Object[source.size()];
			int idx = 0;
			for(Object elem : source)
				result[idx++] = resolveTypeRefs(elem);
			return Helpers.asList(result);
		}

		return value instanceof AnyType ? ((AnyType)value).resolve() : value;
	}

	public Map<String,Object> _pcoreInitHash() {
		Map<String,Object> result = new LinkedHashMap<>();
		Map<AnyType,Map<String,?>> annotations = getAnnotations();
		if(!annotations.isEmpty())
			result.put(Constants.KEY_ANNOTATIONS, annotations);
		return result;
	}

	void setInitHashExpression(Map<String,Object> unresolvedInitHash) {
		this.initHashExpression = unresolvedInitHash;
	}
}
