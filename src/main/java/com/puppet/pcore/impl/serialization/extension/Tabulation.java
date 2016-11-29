package com.puppet.pcore.impl.serialization.extension;

public class Tabulation implements NotTabulated {
	public final int index;

	public Tabulation(int index) {
		this.index = index;
	}

	public boolean equals(Object o) {
		return o != null && getClass().equals(o.getClass()) && index == ((Tabulation)o).index;
	}

	public int hashCode() {
		return index * 17;
	}
}
