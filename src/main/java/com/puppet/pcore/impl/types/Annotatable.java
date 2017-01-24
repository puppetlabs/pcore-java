package com.puppet.pcore.impl.types;

import java.util.Map;

import static com.puppet.pcore.impl.types.TypeFactory.*;

interface Annotatable {
	AnyType TYPE_ANNOTATION_KEY_TYPE = typeType(); // TBD
	AnyType TYPE_ANNOTATION_VALUE_TYPE = structType(); // TBD
	AnyType TYPE_ANNOTATIONS = hashType(TYPE_ANNOTATION_KEY_TYPE, TYPE_ANNOTATION_VALUE_TYPE);

	Map<AnyType,Map<String,?>> getAnnotations();
}
