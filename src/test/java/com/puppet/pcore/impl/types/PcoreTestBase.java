package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.TypeEvaluator;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;

public abstract class PcoreTestBase {
	Pcore pcore;

	@BeforeEach
	public void init() {
		pcore = null;
	}

	public Pcore pcore() {
		if(pcore == null)
			pcore = Pcore.create();
		return pcore;
	}

	public void setPcore(Pcore pcore) {
		this.pcore = pcore;
	}

	public TypeEvaluator typeEvaluator() {
		return pcore().typeEvaluator();
	}

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
