package com.puppet.pcore.impl.serialization.extension;

public class ArrayStart implements NotTabulated, SequenceStart {
	public final int size;

	public ArrayStart(int size) {
		this.size = size;
	}

	public boolean equals(Object o) {
		return o instanceof ArrayStart && size == ((ArrayStart)o).size;
	}

	public int hashCode() {
		return size * 29;
	}

	/**
	 * Sequence size is the same as the size since each entry is one value
	 *
	 * @return the size
	 */
	@Override
	public int sequenceSize() {
		return size;
	}
}
