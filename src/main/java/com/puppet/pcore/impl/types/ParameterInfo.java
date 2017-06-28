package com.puppet.pcore.impl.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.types.TypeFactory.tupleType;

public class ParameterInfo {
	public final Map<String,Integer> attributeIndex;
	public final List<ObjectType.Attribute> attributes;
	public final int[] equalityAttributeIndexes;
	public final int requiredCount;

	ParameterInfo(List<ObjectType.Attribute> attributes, int requiredCount, List<String> equality) {
		Map<String,Integer> attributeIndex = new HashMap<>();
		int idx = attributes.size();
		while(--idx >= 0)
			attributeIndex.put(attributes.get(idx).name, idx);

		this.attributes = Collections.unmodifiableList(attributes);
		this.attributeIndex = Collections.unmodifiableMap(attributeIndex);
		this.requiredCount = requiredCount;

		int top = equality.size();
		int[] ei = new int[top];
		for(idx = 0; idx < top; ++idx)
			ei[idx] = attributeIndex.get(equality.get(idx));
		this.equalityAttributeIndexes = ei;
	}

	public TupleType parametersType() {
		return tupleType(map(attributes, attr -> attr.type), requiredCount, attributes.size());
	}
}
