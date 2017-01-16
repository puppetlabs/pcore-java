package com.puppet.pcore.impl.serialization.extension;

public class ObjectStart implements SequenceStart {
	public final int attributeCount;

	public ObjectStart(int attributeCount) {
		this.attributeCount = attributeCount;
	}

	public boolean equals(Object o) {
		return o instanceof ObjectStart && attributeCount == ((ObjectStart)o).attributeCount;
	}

	public int hashCode() {
		return attributeCount * 3;
	}

	/**
	 * The same as the attributeCount since each attribute is one value
	 *
	 * @return the attributeCount
	 */
	@Override
	public int sequenceSize() {
		return attributeCount;
	}
}
