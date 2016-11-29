package com.puppet.pcore.impl.types;

import com.puppet.pcore.impl.Constants;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.puppet.pcore.impl.types.TypeFactory.*;

interface Annotatable {
	AnyType TYPE_ANNOTATION_KEY_TYPE = typeType(); // TBD
	AnyType TYPE_ANNOTATION_VALUE_TYPE = structType(); // TBD
	AnyType TYPE_ANNOTATIONS = hashType(TYPE_ANNOTATION_KEY_TYPE, TYPE_ANNOTATION_VALUE_TYPE);

	default void annotatableAccept(ModelObject.Visitor visitor, ModelObject.RecursionGuard guard) {
		getAnnotations().keySet().forEach(key -> key.accept(visitor, guard));
	}

	Map<AnyType,Map<String,?>> getAnnotations();

	default Map<String,Object> i12nHash() {
		Map<String,Object> result = new LinkedHashMap<>();
		Map<AnyType,Map<String,?>> annotations = getAnnotations();
		if(!annotations.isEmpty())
			result.put(Constants.KEY_ANNOTATIONS, annotations);
		return result;
	}
}
