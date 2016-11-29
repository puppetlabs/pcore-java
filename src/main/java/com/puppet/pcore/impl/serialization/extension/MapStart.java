package com.puppet.pcore.impl.serialization.extension;

public class MapStart implements NotTabulated, SequenceStart {
	public final int size;

	public MapStart(int size) {
		this.size = size;
	}

	public boolean equals(Object o) {
		return o instanceof MapStart && size == ((MapStart)o).size;
	}

	public int hashCode() {
		return size * 31;
	}

	/**
	 * Sequence size is twice the map size since each entry is written as key and value
	 *
	 * @return the size * 2
	 */
	@Override
	public int sequenceSize() {
		return size;
	}
}
