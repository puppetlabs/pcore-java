package com.puppet.pcore.impl.pn;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.Helpers.MapEntry;

import java.util.Map.Entry;

abstract class AbstractPN  implements PN {

	@Override
	public PN asCall(String name) {
		return new CallPN(name, this);
	}

	@Override
	public Entry<String,PN> withName(String name) {
		return new MapEntry<>(name, this);
	}

	public String toString() {
		StringBuilder bld = new StringBuilder();
		format(bld);
		return bld.toString();
	}
}
