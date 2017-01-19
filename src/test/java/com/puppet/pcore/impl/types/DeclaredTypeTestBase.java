package com.puppet.pcore.impl.types;

import java.net.URI;

import static com.puppet.pcore.Pcore.typeEvaluator;

public abstract class DeclaredTypeTestBase {
	AnyType declareType(String alias, String typeString) {
		return (AnyType)typeEvaluator().declareType(alias, typeString);
	}

	AnyType declareType(String name, String typeString, URI ns) {
		return (AnyType)typeEvaluator().declareType(name, typeString, ns);
	}

	AnyType resolveType(String typeString) {
		return (AnyType)typeEvaluator().resolveType(typeString);
	}
}
