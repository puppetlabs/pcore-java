package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.TypeEvaluator;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.TypeEvaluatorImpl;
import com.puppet.pcore.impl.loader.BasicLoader;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;

import static com.puppet.pcore.Pcore.typeEvaluator;

abstract class DeclaredTypeTest {
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
